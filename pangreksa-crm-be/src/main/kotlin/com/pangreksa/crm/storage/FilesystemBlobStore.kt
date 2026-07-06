package com.pangreksa.crm.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Filesystem-backed [BlobStore]. Blobs are written under `pangreksa.storage.path`
 * (env `PANGREKSA_STORAGE_PATH`, mounted as a Docker volume in deployments). Keys are treated as
 * relative paths and confined to the root to prevent traversal.
 */
@Service
class FilesystemBlobStore(
    @Value("\${pangreksa.storage.path:./storage}") root: String,
) : BlobStore {

    private val base: Path = Paths.get(root).toAbsolutePath().normalize()

    init {
        Files.createDirectories(base)
    }

    private fun resolve(key: String): Path {
        val p = base.resolve(key).normalize()
        require(p.startsWith(base)) { "Invalid storage key: $key" }
        return p
    }

    override fun put(key: String, data: InputStream, contentType: String?) {
        val p = resolve(key)
        Files.createDirectories(p.parent)
        Files.newOutputStream(p).use { out -> data.copyTo(out) }
    }

    override fun get(key: String): InputStream = Files.newInputStream(resolve(key))

    override fun delete(key: String) {
        Files.deleteIfExists(resolve(key))
    }
}
