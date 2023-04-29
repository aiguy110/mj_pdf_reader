package com.gitlab.mudlej.MjPdfReader.manager.database

import com.gitlab.mudlej.MjPdfReader.repository.AppDatabase
import com.gitlab.mudlej.MjPdfReader.repository.PdfRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class DatabaseManagerImpl(private val database: AppDatabase): DatabaseManager {

    override suspend fun saveRecordInBackground(pdfRecord: PdfRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            database.savedLocationDao().insert(pdfRecord)
        }
    }

    override suspend fun findPageNumber(fileHash: String): Int {
        return withContext(Dispatchers.IO) {
            database.savedLocationDao().findSavedPage(fileHash) ?: 0
        }
    }

    override suspend fun setPageNumber(fileHash: String, page: Int) {
        withContext(Dispatchers.IO) {
            database.savedLocationDao().updatePageNumber(fileHash, page)
        }
    }

    override suspend fun hasRecord(fileHash: String): Boolean {
        return withContext(Dispatchers.IO) {
            database.savedLocationDao().hasRecord(fileHash)
        }
    }

    override suspend fun setLastOpened(fileHash: String, lastOpened: LocalDateTime) {
        return withContext(Dispatchers.IO) {
            database.savedLocationDao().updateLastOpened(fileHash, lastOpened)
        }
    }
}