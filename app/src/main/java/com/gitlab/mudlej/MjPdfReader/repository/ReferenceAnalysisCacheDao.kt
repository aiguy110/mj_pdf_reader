package com.gitlab.mudlej.MjPdfReader.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReferenceAnalysisCacheDao {

    @Query("SELECT * FROM ReferenceAnalysisCache WHERE fileHash = :fileHash AND windowKey = :windowKey LIMIT 1")
    fun find(fileHash: String, windowKey: String): ReferenceAnalysisCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: ReferenceAnalysisCache)
}
