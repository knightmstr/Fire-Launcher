package com.knightmstr.firelauncher.home

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher

/**
 * One-time setup without a phone or computer: with ADB debugging on, the Fire TV accepts ADB
 * connections on port 5555, including from itself. FireLauncher connects to 127.0.0.1:5555, the TV
 * asks "Allow USB debugging?", and once allowed FireLauncher grants itself WRITE_SECURE_SETTINGS
 * (the same `pm grant` the phone method runs).
 *
 * A minimal ADB client: connect, sign in with an RSA key kept in the app's files, run one shell
 * command. Key format and signing follow AOSP's adb and dadb (github.com/mobile-dev-inc/dadb,
 * Apache 2.0).
 */
object SelfAdb {

    sealed interface Result {
        data object Granted : Result
        data object AdbOff : Result
        data object NotAllowed : Result
        data class Failed(val reason: String) : Result
    }

    private const val HOST = "127.0.0.1"
    private const val PORT = 5555
    private const val APPROVAL_WAIT_MS = 60_000

    fun isAdbOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    /** Developer options if this TV can open it directly, else the Fire TV settings menu. */
    fun openDeveloperOptions(context: Context): Boolean {
        HomeWatch.allowAmazonBriefly()
        val candidates = listOf(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
            Intent().setComponent(ComponentName("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity")),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }

    /** Connects to this TV's own ADB and grants the permission. Waits up to a minute for Allow. */
    suspend fun grantSelf(context: Context): Result = withContext(Dispatchers.IO) {
        if (HomeWatch.canManage(context)) return@withContext Result.Granted
        if (!isAdbOn(context)) return@withContext Result.AdbOff
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(HOST, PORT), 3000)
                socket.soTimeout = APPROVAL_WAIT_MS
                val conn = Connection(socket, keys(context))
                conn.handshake()
                socket.soTimeout = 15_000
                conn.shell("pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS")
            }
            if (HomeWatch.canManage(context)) Result.Granted
            else Result.Failed("The TV ran the command but didn't grant it")
        } catch (_: SocketTimeoutException) {
            Result.NotAllowed
        } catch (e: NotAllowedException) {
            Result.NotAllowed
        } catch (e: IOException) {
            // Refused connection: ADB debugging is on but not listening on the network port
            Result.Failed(e.message ?: "Couldn't connect to this TV's ADB")
        } catch (e: Exception) {
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private class NotAllowedException : IOException("Not allowed on the TV")

    // ------------------------------------------------------------------ Keys

    private class Keys(val private: PrivateKey, val publicAdb: ByteArray)

    private fun keys(context: Context): Keys {
        val dir = File(context.filesDir, "adb").apply { mkdirs() }
        val priv = File(dir, "adbkey")
        val pub = File(dir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) {
            val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
            priv.writeBytes(pair.private.encoded)
            val adbFormat = Base64.encodeToString(adbPublicKey(pair.public as RSAPublicKey), Base64.NO_WRAP)
            pub.writeText("$adbFormat FireLauncher@firetv")
        }
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(priv.readBytes()))
        return Keys(privateKey, pub.readBytes() + 0)
    }

    /** The RSAPublicKey struct adb stores in adbkey.pub (from AOSP adb_auth / dadb). */
    private fun adbPublicKey(key: RSAPublicKey): ByteArray {
        val words = 2048 / 32
        val r32 = BigInteger.ZERO.setBit(32)
        var n = key.modulus
        var rr = BigInteger.ZERO.setBit(words * 32).modPow(BigInteger.valueOf(2), n)
        val n0inv = n.remainder(r32).modInverse(r32)
        val nWords = IntArray(words)
        val rrWords = IntArray(words)
        for (i in 0 until words) {
            rr.divideAndRemainder(r32).let { rr = it[0]; rrWords[i] = it[1].toInt() }
            n.divideAndRemainder(r32).let { n = it[0]; nWords[i] = it[1].toInt() }
        }
        return ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(words)
            putInt(n0inv.negate().toInt())
            nWords.forEach { putInt(it) }
            rrWords.forEach { putInt(it) }
            putInt(key.publicExponent.toInt())
        }.array()
    }

    /** PKCS#1 v1.5 padding with the SHA-1 DigestInfo prefix, for a 20-byte token and 2048-bit key. */
    private val SIGNATURE_PADDING: ByteArray = ByteArray(256 - 20).also { p ->
        val digestInfo = intArrayOf(0x00, 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14)
        p[0] = 0x00; p[1] = 0x01
        for (i in 2 until p.size - digestInfo.size) p[i] = 0xff.toByte()
        digestInfo.forEachIndexed { i, b -> p[p.size - digestInfo.size + i] = b.toByte() }
    }

    // ------------------------------------------------------------------ Protocol

    private class Message(val command: Int, val arg0: Int, val arg1: Int, val payload: ByteArray)

    private class Connection(socket: Socket, private val keys: Keys) {
        private val input = DataInputStream(socket.getInputStream().buffered())
        private val output: OutputStream = socket.getOutputStream()

        fun handshake() {
            write(CNXN, VERSION, MAX_DATA, "host::\u0000".toByteArray())
            var sentKey = false
            while (true) {
                val m = read()
                when {
                    m.command == CNXN -> return
                    m.command == AUTH && m.arg0 == AUTH_TOKEN && !sentKey -> {
                        // First try a signature; if adbd doesn't know the key it sends another token
                        if (signedOnce) {
                            write(AUTH, AUTH_RSA_PUBLIC, 0, keys.publicAdb)
                            sentKey = true // the TV now shows "Allow USB debugging?"
                        } else {
                            write(AUTH, AUTH_SIGNATURE, 0, sign(m.payload))
                            signedOnce = true
                        }
                    }
                    m.command == AUTH -> throw NotAllowedException()
                    else -> throw IOException("Unexpected reply from ADB")
                }
            }
        }

        private var signedOnce = false

        fun shell(command: String): String {
            val localId = 1
            write(OPEN, localId, 0, "shell:$command\u0000".toByteArray())
            val out = StringBuilder()
            var remoteId = 0
            while (true) {
                val m = read()
                when (m.command) {
                    OKAY -> remoteId = m.arg0
                    WRTE -> {
                        out.append(String(m.payload))
                        write(OKAY, localId, m.arg0, ByteArray(0))
                    }
                    CLSE -> {
                        if (remoteId != 0) write(CLSE, localId, remoteId, ByteArray(0))
                        return out.toString()
                    }
                }
            }
        }

        private fun sign(token: ByteArray): ByteArray =
            Cipher.getInstance("RSA/ECB/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, keys.private)
                update(SIGNATURE_PADDING)
                doFinal(token)
            }

        private fun write(command: Int, arg0: Int, arg1: Int, payload: ByteArray) {
            val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(command); putInt(arg0); putInt(arg1)
                putInt(payload.size)
                putInt(payload.sumOf { it.toInt() and 0xff })
                putInt(command xor -1)
            }.array()
            output.write(header + payload)
            output.flush()
        }

        private fun read(): Message {
            val header = ByteArray(24).also { input.readFully(it) }
            val b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val command = b.int; val arg0 = b.int; val arg1 = b.int; val length = b.int
            if (length < 0 || length > MAX_DATA) throw IOException("Bad ADB message")
            val payload = ByteArray(length).also { input.readFully(it) }
            return Message(command, arg0, arg1, payload)
        }

        companion object {
            const val CNXN = 0x4e584e43
            const val AUTH = 0x48545541
            const val OPEN = 0x4e45504f
            const val OKAY = 0x59414b4f
            const val CLSE = 0x45534c43
            const val WRTE = 0x45545257
            const val VERSION = 0x01000000
            const val MAX_DATA = 256 * 1024
            const val AUTH_TOKEN = 1
            const val AUTH_SIGNATURE = 2
            const val AUTH_RSA_PUBLIC = 3
        }
    }
}
