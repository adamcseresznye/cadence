package com.cadence.daf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PracticeLogStore(context: Context) {
    private val file = File(context.filesDir, "practice.csv")

    suspend fun append(line: String) = withContext(Dispatchers.IO) {
        file.appendText(line + "\n")
    }

    suspend fun readLines(): List<String> = withContext(Dispatchers.IO) {
        if (file.exists()) file.readLines() else emptyList()
    }

    suspend fun clear() = withContext(Dispatchers.IO) { file.delete(); Unit }
}
