package com.example.data

import com.example.BuildConfig
import com.example.supabase
import io.github.jan.supabase.storage.storage

object EsmeryStorage {
  suspend fun upload(bucket: String, path: String, bytes: ByteArray): String {
    val storage = supabase.storage.from(bucket)
    storage.upload(path, bytes) { upsert = true }
    return "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/$bucket/$path"
  }
}
