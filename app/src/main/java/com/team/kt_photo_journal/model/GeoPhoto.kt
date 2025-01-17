package com.team.kt_photo_journal.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.time.LocalDateTime

@Entity(tableName = "geo_photo_table")
data class GeoPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    @ColumnInfo(name = "filePath") val filepath: String,
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "latitude") var latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "timestamp") val timestamp: LocalDateTime
) : Serializable