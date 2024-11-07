package com.team.kt_photo_journal.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "geo_photo_table")
data class GeoPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    @ColumnInfo(name = "filePath") val filepath: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "latitude") var latitude: Long,
    @ColumnInfo(name = "longitude") val longitude: Long,
    @ColumnInfo(name = "timestamp") val dueDate: LocalDateTime
)