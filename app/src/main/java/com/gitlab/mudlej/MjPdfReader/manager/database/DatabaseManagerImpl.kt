package com.gitlab.mudlej.MjPdfReader.manager.database

import com.gitlab.mudlej.MjPdfReader.data.llm.PageAnalysisResult
import com.gitlab.mudlej.MjPdfReader.enums.ReadingStatus
import com.gitlab.mudlej.MjPdfReader.repository.AppDatabase
import com.gitlab.mudlej.MjPdfReader.repository.PdfRecord
import com.gitlab.mudlej.MjPdfReader.repository.ReferenceAnalysisCache
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class DatabaseManagerImpl(private val database: AppDatabase): DatabaseManager {

    private val gson = Gson()

    override suspend fun findAllRecords(): List<PdfRecord> {
        return withContext(Dispatchers.IO) {
            database.pdfRecordDao().findAll()
        }
    }

    override suspend fun saveRecordInBackground(pdfRecord: PdfRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            database.pdfRecordDao().insert(pdfRecord)
        }
    }

    override suspend fun findPageNumber(fileHash: String): Int {
        return withContext(Dispatchers.IO) {
            database.pdfRecordDao().findSavedPage(fileHash) ?: 0
        }
    }

    override suspend fun findPdfPassword(fileHash: String): String? {
        return withContext(Dispatchers.IO) {
            database.pdfRecordDao().findPdfPassword(fileHash)
        }
    }

    override suspend fun setPageNumber(fileHash: String, page: Int) {
        withContext(Dispatchers.IO) {
            database.pdfRecordDao().updatePageNumber(fileHash, page)
        }
    }

    override suspend fun hasRecord(fileHash: String): Boolean {
        return withContext(Dispatchers.IO) {
            database.pdfRecordDao().hasRecord(fileHash)
        }
    }

    override suspend fun setLastOpened(fileHash: String, lastOpened: LocalDateTime) {
        return withContext(Dispatchers.IO) {
            database.pdfRecordDao().updateLastOpened(fileHash, lastOpened)
        }
    }

    override suspend fun removeRecord(record: PdfRecord) {
        withContext(Dispatchers.IO) {
            database.pdfRecordDao().delete(record)
        }
    }

    override suspend fun setFavorite(fileHash: String, favorite: Boolean) {
        withContext(Dispatchers.IO) {
            database.pdfRecordDao().updateFavorite(fileHash, favorite)
        }
    }

    override suspend fun setReading(fileHash: String, readingStatus: ReadingStatus) {
        withContext(Dispatchers.IO) {
            database.pdfRecordDao().updateReading(fileHash, readingStatus)
        }
    }

    override suspend fun setPassword(fileHash: String, password: String) {
        withContext(Dispatchers.IO) {
            database.pdfRecordDao().updatePassword(fileHash, password)
        }
    }

    override suspend fun findReferenceAnalysis(fileHash: String, windowKey: String): PageAnalysisResult? {
        return withContext(Dispatchers.IO) {
            database.referenceAnalysisCacheDao().find(fileHash, windowKey)
                ?.let { gson.fromJson(it.resultJson, PageAnalysisResult::class.java) }
        }
    }

    override suspend fun saveReferenceAnalysis(
        fileHash: String,
        windowKey: String,
        centerPage: Int,
        result: PageAnalysisResult,
    ) {
        withContext(Dispatchers.IO) {
            database.referenceAnalysisCacheDao().insert(
                ReferenceAnalysisCache(
                    fileHash = fileHash,
                    windowKey = windowKey,
                    centerPage = centerPage,
                    resultJson = gson.toJson(result),
                    createdAt = LocalDateTime.now(),
                )
            )
        }
    }

}