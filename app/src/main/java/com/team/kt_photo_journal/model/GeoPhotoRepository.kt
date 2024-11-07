package com.team.kt_photo_journal.model

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class GeoPhotoRepository(private val geoPhotoDao: GeoPhotoDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allGeoPhotos: Flow<Map<Int,GeoPhoto>> = geoPhotoDao.getAllGeoPhotos()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun insert(geoPhoto: GeoPhoto) {
        geoPhotoDao.insert(geoPhoto)
    }

    @WorkerThread
    suspend fun update(geoPhoto: GeoPhoto){
        geoPhotoDao.update(geoPhoto)
    }

    @WorkerThread
    suspend fun delete(geoPhoto: GeoPhoto){
        geoPhotoDao.delete(geoPhoto.id!!)
    }
}