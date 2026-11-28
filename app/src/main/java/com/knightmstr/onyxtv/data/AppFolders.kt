package com.knightmstr.onyxtv.data

/** A named group of apps that sits in the dock as one tile. */
data class AppFolder(val id: String, val name: String, val packages: List<String>)

/**
 * Pure folder logic: storage format and every change, each returning the new folders and the
 * new favourites order together, so the two can never disagree.
 *
 * A folder is in the dock as the favourites entry `folder:<id>`. An app moved into a folder
 * leaves the dock (it's inside the folder tile now) but stays in All Apps.
 */
object AppFolders {

    const val KEY_PREFIX = "folder:"
    const val MAX_FOLDERS = 20
    const val MAX_APPS_PER_FOLDER = 60
    const val MAX_NAME_LENGTH = 30
    private val ID = Regex("^f[0-9]{1,6}$")

    data class State(val folders: List<AppFolder>, val favorites: List<String>)

    fun keyOf(id: String) = KEY_PREFIX + id
    fun isFolderKey(key: String) = key.startsWith(KEY_PREFIX) && ID.matches(key.removePrefix(KEY_PREFIX))
    fun idOf(key: String): String? = key.removePrefix(KEY_PREFIX).takeIf { isFolderKey(key) }

    fun cleanName(name: String): String =
        name.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim().take(MAX_NAME_LENGTH)

    // ---------------------------------------------------------------- storage

    /** One folder per line: `id<TAB>name<TAB>pkg,pkg,…`. Bad lines are skipped. */
    fun encode(folders: List<AppFolder>): String = folders.joinToString("\n") { f ->
        "${f.id}\t${cleanName(f.name)}\t${f.packages.joinToString(",")}"
    }

    fun decode(raw: String?): List<AppFolder> {
        if (raw.isNullOrBlank()) return emptyList()
        val out = ArrayList<AppFolder>()
        val ids = HashSet<String>()
        for (line in raw.split('\n')) {
            val parts = line.split('\t')
            if (parts.size != 3) continue
            val id = parts[0]
            if (!ID.matches(id) || !ids.add(id)) continue
            val name = cleanName(parts[1]).ifEmpty { "Folder" }
            val pkgs = parts[2].split(',').map { it.trim() }.filter { PACKAGE.matches(it) }.distinct().take(MAX_APPS_PER_FOLDER)
            out += AppFolder(id, name, pkgs)
            if (out.size >= MAX_FOLDERS) break
        }
        return out
    }

    private val PACKAGE = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*$")

    // ---------------------------------------------------------------- changes

    private fun nextId(folders: List<AppFolder>): String {
        val used = folders.mapNotNull { it.id.removePrefix("f").toIntOrNull() }.toSet()
        var n = 1
        while (n in used) n++
        return "f$n"
    }

    /** Next default name: "Folder", "Folder 2", … */
    fun defaultName(folders: List<AppFolder>): String {
        val names = folders.map { it.name }.toSet()
        if ("Folder" !in names) return "Folder"
        var n = 2
        while ("Folder $n" in names) n++
        return "Folder $n"
    }

    /** New folder holding [pkg]; it takes the app's place in the dock (or goes at the end). */
    fun create(state: State, pkg: String, name: String? = null): State {
        if (state.folders.size >= MAX_FOLDERS) return state
        val id = nextId(state.folders)
        val folders = state.folders.map { it.copy(packages = it.packages - pkg) } +
            AppFolder(id, cleanName(name ?: defaultName(state.folders)).ifEmpty { "Folder" }, listOf(pkg))
        val key = keyOf(id)
        val at = state.favorites.indexOf(pkg)
        val favorites = if (at >= 0) state.favorites.toMutableList().apply { set(at, key) } else state.favorites + key
        val (f, fav) = dropEmpty(folders, favorites)
        return State(f, fav)
    }

    /** Puts [pkg] into folder [id] (out of any other folder), and takes it off the dock. */
    fun moveInto(state: State, pkg: String, id: String): State {
        val target = state.folders.firstOrNull { it.id == id } ?: return state
        if (pkg in target.packages || target.packages.size >= MAX_APPS_PER_FOLDER) return state
        val folders = state.folders.map {
            when (it.id) {
                id -> it.copy(packages = it.packages + pkg)
                else -> it.copy(packages = it.packages - pkg)
            }
        }
        val (f, fav) = dropEmpty(folders, state.favorites - pkg)
        return State(f, fav)
    }

    /** Takes [pkg] out of its folder and puts it in the dock right after the folder. */
    fun takeOut(state: State, pkg: String): State {
        val from = state.folders.firstOrNull { pkg in it.packages } ?: return state
        val folders = state.folders.map { if (it.id == from.id) it.copy(packages = it.packages - pkg) else it }
        val favorites = state.favorites.toMutableList()
        favorites.remove(pkg)
        val at = favorites.indexOf(keyOf(from.id))
        if (at >= 0) favorites.add(at + 1, pkg) else favorites.add(pkg)
        val (f, fav) = dropEmpty(folders, favorites)
        return State(f, fav)
    }

    fun rename(state: State, id: String, name: String): State {
        val clean = cleanName(name)
        if (clean.isEmpty()) return state
        return state.copy(folders = state.folders.map { if (it.id == id) it.copy(name = clean) else it })
    }

    /** Removes the folder; its apps go back into the dock where the folder was. */
    fun delete(state: State, id: String): State {
        val folder = state.folders.firstOrNull { it.id == id } ?: return state
        val favorites = state.favorites.toMutableList()
        val at = favorites.indexOf(keyOf(id))
        val apps = folder.packages.filter { it !in favorites }
        if (at >= 0) {
            favorites.removeAt(at)
            favorites.addAll(at, apps)
        } else {
            favorites.addAll(apps)
        }
        return State(state.folders.filter { it.id != id }, favorites)
    }

    /** Folders left empty disappear, along with their dock entry; dock entries of missing folders too. */
    private fun dropEmpty(folders: List<AppFolder>, favorites: List<String>): Pair<List<AppFolder>, List<String>> {
        val kept = folders.filter { it.packages.isNotEmpty() }
        val keys = kept.map { keyOf(it.id) }.toSet()
        return kept to favorites.filter { !isFolderKey(it) || it in keys }
    }

    /** The folder holding [pkg], if any. */
    fun folderOf(folders: List<AppFolder>, pkg: String): AppFolder? = folders.firstOrNull { pkg in it.packages }
}
