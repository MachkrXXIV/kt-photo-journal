package com.team.kt_photo_journal

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.team.kt_photo_journal.model.GeoPhoto
import com.team.kt_photo_journal.util.LocationUtilCallback
import com.team.kt_photo_journal.util.createLocationCallback
import com.team.kt_photo_journal.util.createLocationRequest
import com.team.kt_photo_journal.util.replaceFragmentInActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.OutputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "MainActivity"

    //mapsFragment object to hold the fragment
    private lateinit var mapsFragment: OpenStreetMapFragment

    //Boolean to keep track of whether permissions have been granted
    private var locationPermissionEnabled: Boolean = false

    //Boolean to keep track of whether activity is currently requesting location Updates
    private var locationRequestsEnabled: Boolean = false

    //Member object for the FusedLocationProvider
    private lateinit var locationProviderClient: FusedLocationProviderClient

    //Member object for the last known location
    private lateinit var mCurrentLocation: Location

    //Member object to hold onto locationCallback object
    //Needed to remove requests for location updates
    private lateinit var mLocationCallback: LocationCallback

    //ViewModel object to communicate between Activity and repository
    private val photoJournalViewModel: PhotoJournalViewModel by viewModels {
        PhotoJournalViewModelFactory((application as PhotoJournalApplication).repository)
    }

    // camera
    var currentPhotoPath = ""
    val takePictureResultLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()){
            result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_CANCELED){
            Log.d(LOG_TAG,"Picture Intent Cancelled")
        }else{
            // just pass data through intents
            // save stuff to database in GeoPhotoActivity
            val geoPhoto = GeoPhoto(
                id = -1,
                latitude = mCurrentLocation.latitude,
                description = "Enter a description",
                longitude = mCurrentLocation.longitude,
                filepath = currentPhotoPath,
                timestamp = LocalDateTime.now()
            )
            galleryAddPic(currentPhotoPath)
//            photoJournalViewModel.insert(geoPhoto)
            launchNewGeoPhotoActivity()
            Log.d(LOG_TAG,"Picture Successfully taken at $currentPhotoPath")
        }

    }

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            //If successful, startLocationRequests
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                locationPermissionEnabled = true
                startLocationRequests()
            }
            //If successful at coarse detail, we still want those
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                locationPermissionEnabled = true
                startLocationRequests()
            }

            else -> {
                //Otherwise, send toast saying location is not enabled
                locationPermissionEnabled = false
                Toast.makeText(this, "Location Not Enabled", Toast.LENGTH_LONG)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val fab = findViewById<FloatingActionButton>(R.id.camera)
        fab.setOnClickListener {
            Log.d(LOG_TAG, "Camera Button Clicked")
            takeAPicture()
        }
        Configuration.getInstance().load(
            this, getSharedPreferences(
                "${packageName}_preferences", Context.MODE_PRIVATE
            )
        )
        mapsFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
                as OpenStreetMapFragment? ?:OpenStreetMapFragment.newInstance().also{
            replaceFragmentInActivity(it,R.id.fragment_container_view)
        }
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        checkForLocationPermission()
        photoJournalViewModel.allGeoPhotos.observe(this, Observer {
            Log.d("MainActivity", "GeoPhotos: $it")
            it.forEach { geoPhoto ->
                val geoPoint = GeoPoint(geoPhoto.value.latitude, geoPhoto.value.longitude)
                mapsFragment.addMarker(geoPoint, geoPhoto.value.id!!)
            }
        })

    }

    private fun checkForLocationPermission(){
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationRequests()
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
    }

    //LocationUtilCallback object
    //Dynamically defining two results from locationUtils
    //Namely requestPermissions and locationUpdated
    private val locationUtilCallback = object : LocationUtilCallback {
        //If locationUtil request fails because of permission issues
        //Ask for permissions
        override fun requestPermissionCallback() {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        //If locationUtil returns a Location object
        //Populate the current location and log
        override fun locationUpdatedCallback(location: Location) {
            mCurrentLocation = location
            mapsFragment.changeCenterLocation(GeoPoint(location.latitude, location.longitude))
            Log.d(
                LOG_TAG,
                "Location is [Lat: ${location.latitude}, Long: ${location.longitude}]"
            )
        }
    }

    private fun startLocationRequests() {
        //If we aren't currently getting location updates
        if (!locationRequestsEnabled) {
            //create a location callback
            mLocationCallback = createLocationCallback(locationUtilCallback)
            //and request location updates, setting the boolean equal to whether this was successful
            locationRequestsEnabled =
                createLocationRequest(this, locationProviderClient, mLocationCallback)
        }
    }

    private fun createFilePath(): String {
        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intent
        return image.absolutePath
    }

    private fun takeAPicture(){
        Log.d(LOG_TAG,"Taking Picture")
        val pictureIntent: Intent = Intent().setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        if(pictureIntent.resolveActivity(packageManager)!=null){
            val filepath = createFilePath()
            val myFile: File = File(filepath)
            currentPhotoPath = filepath
            val photoUri = FileProvider.getUriForFile(this,"com.team.kt_photo_journal.fileprovider",myFile)
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
            takePictureResultLauncher.launch(pictureIntent)
        }
    }

    private fun launchNewGeoPhotoActivity(id: Int = -1) {
        Log.d(LOG_TAG, "Launching new GeoPhoto Activity")
        val secondActivityIntent = Intent(this, GeoPhotoActivity::class.java)
        secondActivityIntent.putExtra("EXTRA_ID", id)
        secondActivityIntent.putExtra("EXTRA_LATITUDE", mCurrentLocation.latitude)
        secondActivityIntent.putExtra("EXTRA_LONGITUDE", mCurrentLocation.longitude)
        secondActivityIntent.putExtra("EXTRA_FILEPATH", currentPhotoPath)
//        secondActivityIntent.putExtra("EXTRA_TIMESTAMP", LocalDateTime.now().toString())
        this.startActivity(secondActivityIntent)
    }

    @WorkerThread
    private fun galleryAddPic(filename:String) {
        //Make sure to call this function on a worker thread, else it will block main thread
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = application.contentResolver

        contentResolver.also { resolver ->
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }
        val bitmap = BitmapFactory.decodeFile(currentPhotoPath)

        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        imageUri?.let { contentResolver.update(it, contentValues, null, null) }

    }
}