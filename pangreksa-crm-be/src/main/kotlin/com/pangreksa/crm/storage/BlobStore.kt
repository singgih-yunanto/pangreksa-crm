package com.pangreksa.crm.storage

import java.io.InputStream

/**
 * Binary storage abstraction for attachments. The DB keeps only metadata (filename, size, content
 * type, and an opaque [storage key][key]); the bytes live behind this interface. The default
 * implementation is [FilesystemBlobStore]; a future S3/object-storage impl can be dropped in via
 * configuration without touching any caller.
 */
interface BlobStore {
    /** Store [data] under [key], overwriting any existing blob at that key. */
    fun put(key: String, data: InputStream, contentType: String?)

    /** Open a stream to read the blob at [key]. Caller closes it. */
    fun get(key: String): InputStream

    /** Remove the blob at [key] (no-op if absent). */
    fun delete(key: String)
}
