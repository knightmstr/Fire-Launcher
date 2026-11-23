package com.knightmstr.onyxtv.upload

import com.knightmstr.onyxtv.data.LocalPhotoSource
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * A tiny, token-gated HTTP server that lets any device on the same Wi-Fi upload wallpaper photos
 * to the TV from a browser — no adb, no account. It only runs while the "Upload photos" screen is
 * open (the ViewModel starts and stops it), and every action is confined to the app-private upload
 * folder via [LocalPhotoSource].
 *
 * Security model:
 *  - A random [token] is minted per session and shown on the TV as part of the URL. Every API call
 *    must carry it (`?t=<token>`); without it the server answers 403. This keeps a random person on
 *    the same network from blindly posting or deleting.
 *  - Uploads are validated as real images and capped in size/count; file names are generated, never
 *    trusted from the client, so there is no path traversal.
 *  - Traffic is plain HTTP on the LAN (no certificate on a home TV); fine for personal photos.
 */
class PhotoUploadServer(
    port: Int,
    private val token: String,
    private val photos: LocalPhotoSource,
    private val onChanged: () -> Unit
) : NanoHTTPD(port) {

    fun startServer() = start(SOCKET_READ_TIMEOUT, true)

    override fun serve(session: IHTTPSession): Response = try {
        when (session.uri) {
            "/", "/index.html" -> html(PAGE)
            "/api/list" -> guarded(session) { listResponse() }
            "/api/img" -> guarded(session) { imageResponse(session) }
            "/api/upload" -> guarded(session) { uploadResponse(session) }
            "/api/delete" -> guarded(session) { deleteResponse(session) }
            else -> status(Response.Status.NOT_FOUND, "Not found")
        }
    } catch (e: Exception) {
        status(Response.Status.INTERNAL_ERROR, "Server error")
    }

    private inline fun guarded(session: IHTTPSession, block: () -> Response): Response {
        val given = session.parameters["t"]?.firstOrNull()
        return if (given == token) block() else status(Response.Status.FORBIDDEN, "Forbidden")
    }

    private fun listResponse(): Response {
        val array = JSONArray()
        photos.listUploads().forEach { array.put(JSONObject().put("name", it.name)) }
        return json(JSONObject().put("photos", array).toString())
    }

    private fun imageResponse(session: IHTTPSession): Response {
        val name = session.parameters["name"]?.firstOrNull() ?: return status(Response.Status.BAD_REQUEST, "No name")
        val file = photos.listUploads().firstOrNull { it.name == name }
            ?: return status(Response.Status.NOT_FOUND, "Not found")
        return newChunkedResponse(Response.Status.OK, mimeFor(file), FileInputStream(file))
    }

    private fun uploadResponse(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (length > LocalPhotoSource.MAX_UPLOAD_BYTES) {
            return json(JSONObject().put("ok", false).put("error", "too_large").toString())
        }
        val files = HashMap<String, String>()
        session.parseBody(files)
        val tempPath = files["photo"] ?: return json(JSONObject().put("ok", false).put("error", "no_file").toString())
        val originalName = session.parameters["photo"]?.firstOrNull()
        val bytes = File(tempPath).readBytes()
        val saved = photos.saveUpload(bytes, originalName)
        return if (saved != null) {
            onChanged()
            json(JSONObject().put("ok", true).put("name", saved.name).toString())
        } else {
            json(JSONObject().put("ok", false).put("error", "rejected").toString())
        }
    }

    private fun deleteResponse(session: IHTTPSession): Response {
        val name = session.parameters["name"]?.firstOrNull() ?: return status(Response.Status.BAD_REQUEST, "No name")
        val ok = photos.deleteUpload(name)
        if (ok) onChanged()
        return json(JSONObject().put("ok", ok).toString())
    }

    private fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun json(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", body)

    private fun html(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html", body)

    private fun status(code: Response.Status, message: String): Response =
        newFixedLengthResponse(code, "text/plain", message)

    private companion object {
        val PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Onyx TV — Upload photos</title>
<style>
  :root { color-scheme: dark; }
  body { margin:0; font-family:-apple-system,Segoe UI,Roboto,sans-serif; background:#0b0f14; color:#e5e7eb; }
  header { padding:20px 16px; }
  h1 { font-size:20px; margin:0 0 4px; }
  p.sub { margin:0; color:#9ca3af; font-size:13px; }
  main { padding:0 16px 40px; max-width:720px; margin:0 auto; }
  .drop { border:2px dashed #374151; border-radius:14px; padding:28px 16px; text-align:center; }
  .drop input { display:none; }
  .btn { display:inline-block; background:#10b981; color:#04120c; font-weight:600; padding:12px 18px;
         border-radius:10px; border:none; font-size:15px; cursor:pointer; }
  .btn.sec { background:#1f2937; color:#e5e7eb; }
  #status { margin:14px 0; font-size:14px; min-height:20px; }
  .grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(130px,1fr)); gap:10px; margin-top:16px; }
  .card { background:#111827; border-radius:12px; overflow:hidden; }
  .card img { width:100%; height:90px; object-fit:cover; display:block; }
  .card .row { display:flex; justify-content:space-between; align-items:center; padding:6px 8px; gap:6px; }
  .card .name { font-size:11px; color:#9ca3af; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
  .del { background:none; border:none; color:#f87171; font-size:13px; cursor:pointer; }
  .warn { color:#fbbf24; }
</style>
</head>
<body>
<header>
  <h1>Upload photos to Onyx TV</h1>
  <p class="sub">Pick photos below. They become wallpapers on the TV right away.</p>
</header>
<main>
  <div class="drop">
    <label class="btn" for="file">Choose photos</label>
    <input id="file" type="file" accept="image/*" multiple>
    <p class="sub" style="margin-top:12px">JPEG, PNG or WebP · up to 20 MB each</p>
  </div>
  <div id="status"></div>
  <div class="grid" id="grid"></div>
</main>
<script>
  var token = new URLSearchParams(location.search).get('t') || '';
  var statusEl = document.getElementById('status');
  var grid = document.getElementById('grid');

  function api(path) { return path + (path.indexOf('?') < 0 ? '?' : '&') + 't=' + encodeURIComponent(token); }

  function load() {
    if (!token) { statusEl.innerHTML = '<span class="warn">Open the exact link shown on your TV.</span>'; return; }
    fetch(api('/api/list')).then(function(r){ if(!r.ok) throw 0; return r.json(); })
      .then(function(d){ render(d.photos || []); })
      .catch(function(){ statusEl.innerHTML = '<span class="warn">Couldn\'t reach the TV. Open the link shown on it.</span>'; });
  }

  function render(photos) {
    grid.innerHTML = '';
    photos.forEach(function(p){
      var card = document.createElement('div'); card.className = 'card';
      card.innerHTML = '<img src="' + api('/api/img') + '&name=' + encodeURIComponent(p.name) + '">' +
        '<div class="row"><span class="name">' + p.name + '</span>' +
        '<button class="del">Remove</button></div>';
      card.querySelector('.del').onclick = function(){ remove(p.name); };
      grid.appendChild(card);
    });
  }

  function remove(name) {
    fetch(api('/api/delete') + '&name=' + encodeURIComponent(name), { method:'POST' })
      .then(function(){ load(); });
  }

  document.getElementById('file').addEventListener('change', function(e){
    var files = Array.prototype.slice.call(e.target.files);
    if (!files.length) return;
    var done = 0, failed = 0;
    statusEl.textContent = 'Uploading ' + files.length + '…';
    var next = function(i){
      if (i >= files.length) {
        statusEl.textContent = 'Added ' + done + (failed ? (', ' + failed + ' skipped') : '') + '.';
        load(); return;
      }
      var fd = new FormData(); fd.append('photo', files[i]);
      fetch(api('/api/upload'), { method:'POST', body:fd })
        .then(function(r){ return r.json(); })
        .then(function(d){ d && d.ok ? done++ : failed++; })
        .catch(function(){ failed++; })
        .then(function(){ next(i+1); });
    };
    next(0);
  });

  load();
</script>
</body>
</html>
""".trimIndent()
    }
}
