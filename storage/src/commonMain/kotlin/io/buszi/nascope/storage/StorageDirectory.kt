package io.buszi.nascope.storage

import okio.Path

/** Where this platform keeps the app's files. Implemented per platform. */
internal interface StorageDirectory {
    /** Returns the path of [name] inside the app's data directory, creating the directory if needed. */
    fun file(name: String): Path
}
