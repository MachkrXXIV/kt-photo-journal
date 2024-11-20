package com.team.kt_photo_journal

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.team.kt_photo_journal.model.GeoPhoto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class GeoPhotoActivity : AppCompatActivity() {
    private val LOG_TAG = "GeoPhotoActivity"
    private lateinit var etDescription: EditText
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var imageView: ImageView
    private lateinit var geoPhoto: GeoPhoto
    private lateinit var saveBtn: Button
    private val geoPhotoViewModel: GeoPhotoViewModel by viewModels {
        GeoPhotoViewModelFactory((application as PhotoJournalApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_geo_photo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.geo_photo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val id = intent.getIntExtra("EXTRA_ID", -1)
        tvLatitude = findViewById(R.id.latitude)
        tvLongitude = findViewById(R.id.longitude)
        etDescription = findViewById(R.id.description)
        tvTimestamp = findViewById(R.id.timestamp)
        imageView = findViewById(R.id.photo)
        saveBtn = findViewById(R.id.save)
        saveBtn.setOnClickListener() {
            val newGeoPhoto = GeoPhoto(
                geoPhoto.id,
                geoPhoto.filepath,
                etDescription.text.toString(),
                geoPhoto.latitude,
                geoPhoto.longitude,
                geoPhoto.timestamp
            )
            if (geoPhoto.id == null) {
                geoPhotoViewModel.insert(newGeoPhoto)
            } else {
                geoPhotoViewModel.update(newGeoPhoto)
            }
            finish()
        }

        if (id == -1) {
            val latitude = intent.getDoubleExtra("EXTRA_LATITUDE", 0.0)
            val longitude = intent.getDoubleExtra("EXTRA_LONGITUDE", 0.0)
            val timestamp = LocalDateTime.now()
            val filepath = intent.getStringExtra("EXTRA_FILEPATH")
            tvLatitude.text = "Latitude: ${latitude.toString()}"
            tvLongitude.text = "Longitude: ${longitude.toString()}"
            tvTimestamp.text =
                timestamp.format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"))
            imageView.addOnLayoutChangeListener() { _, _, _, _, _, _, _, _, _ ->
                setPic(filepath ?: "")
            }
            geoPhoto = GeoPhoto(
                null,
                filepath ?: "",
                "",
                latitude,
                longitude,
                timestamp
            )
        } else {
            geoPhotoViewModel.start(id)
            geoPhotoViewModel.geoPhoto.observe(this) {
                if (it != null) {
                    geoPhoto = it
                    etDescription.setText(it.description)
                    tvLatitude.text = "Latitude: ${it.latitude.toString()}"
                    tvLongitude.text = "Longitude: ${it.longitude.toString()}"
                    tvTimestamp.text =
                        it.timestamp.format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"))
                    setPic(it.filepath)
                }
            }
        }

    }

    private fun setPic(photoPath: String) {
        if (photoPath.isBlank()) {
            Log.e(LOG_TAG, "Photo path is blank")
            return
        }
        val targetW: Int = imageView.getWidth()

        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoPath, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight
        val photoRatio: Double = (photoH.toDouble()) / (photoW.toDouble())
        val targetH: Int = (targetW * photoRatio).roundToInt()
        // Determine how much to scale down the image
        val scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH / targetH))


        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        val bitmap = BitmapFactory.decodeFile(photoPath, bmOptions)
        imageView.setImageBitmap(bitmap)
    }
}