package com.gitlab.mudlej.MjPdfReader.manager.database

import com.gitlab.mudlej.MjPdfReader.repository.PdfRecord
import java.time.LocalDateTime

interface DatabaseManager {

    suspend fun saveRecordInBackground(pdfRecord: PdfRecord)

    suspend fun findPageNumber(fileHash: String): Int

    suspend fun setPageNumber(fileHash: String, page: Int)

    suspend fun hasRecord(fileHash: String): Boolean

    suspend fun setLastOpened(fileHash: String, lastOpened: LocalDateTime)

}