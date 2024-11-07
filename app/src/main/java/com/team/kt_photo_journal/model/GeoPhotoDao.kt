package com.team.kt_photo_journal.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoPhotoDao {

    @MapInfo(keyColumn = "id")
    @Query("SELECT * FROM geo_photo_table ORDER BY id ASC")
    fun getAllGeoPhotos(): Flow<Map<Int,GeoPhoto>>

    @Update
    suspend fun update(geoPhoto: GeoPhoto)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(geoPhoto: GeoPhoto)

    @Query("DELETE FROM geo_photo_table")
    suspend fun deleteAll()

    @Query("DELETE FROM geo_photo_table WHERE id = :id")
    suspend fun delete(id: Int)
}