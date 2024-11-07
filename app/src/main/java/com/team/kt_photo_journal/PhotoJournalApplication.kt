package com.team.kt_photo_journal

import android.app.Application
import com.team.kt_photo_journal.model.GeoPhotoRepository
import com.team.kt_photo_journal.model.GeoPhotoRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PhotoJournalApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())
    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { GeoPhotoRoomDatabase.getDatabase(this,applicationScope) }
    val repository by lazy { GeoPhotoRepository(database.geoPhotoDao()) }
}