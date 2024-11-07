package com.team.kt_photo_journal.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.team.kt_photo_journal.util.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// Annotates class to be a Room Database with a table (entity) of the Word class
@Database(entities = [GeoPhoto::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
public abstract class GeoPhotoRoomDatabase : RoomDatabase() {

    abstract fun geoPhotoDao(): GeoPhotoDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: GeoPhotoRoomDatabase? = null

        fun getDatabase(context: Context, scope:CoroutineScope): GeoPhotoRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoPhotoRoomDatabase::class.java,
                    "geo_photo_database"
                ).addCallback(GeoPhotoDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
    private class GeoPhotoDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.geoPhotoDao())
                }
            }
        }

        suspend fun populateDatabase(geoPhotoDao: GeoPhotoDao) {
            // Delete all content here.
            geoPhotoDao.deleteAll()

            // Add sample geoPhotos
            var geoPhoto = GeoPhoto(1,"Hello","Description",1, 2, LocalDateTime.now())
            geoPhotoDao.insert(geoPhoto)
            geoPhoto = GeoPhoto(2,"World","Description",2, 4, LocalDateTime.now())
            geoPhotoDao.insert(geoPhoto)
        }
    }
}