package com.gitlab.mudlej.MjPdfReader.repository

import androidx.room.Entity
import java.time.LocalDateTime

@Entity(primaryKeys = ["fileHash", "windowKey"])
data class ReferenceAnalysisCache(
    val fileHash: String,
    // "$windowStart-$windowEnd" for page mode (e.g. "2-4"); literal "full" for document mode.
    val windowKey: String,
    val centerPage: Int,
    val resultJson: String,
    val createdAt: LocalDateTime,
)
