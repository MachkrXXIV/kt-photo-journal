package com.team.kt_photo_journal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.team.kt_photo_journal.model.GeoPhoto
import com.team.kt_photo_journal.model.GeoPhotoRepository
import kotlinx.coroutines.launch

class GeoPhotoViewModel(private val repository: GeoPhotoRepository) : ViewModel() {
    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    val _geoPhoto = MutableLiveData<GeoPhoto>().apply{value=null}
    val geoPhoto: LiveData<GeoPhoto>
        get() = _geoPhoto

    fun start(id:Int){
        viewModelScope.launch {
            repository.allGeoPhotos.collect{
                _geoPhoto.value = it[id]
            }
        }
    }

    fun insert(geoPhoto: GeoPhoto) = viewModelScope.launch {
        repository.insert(geoPhoto)
    }

    fun update(geoPhoto: GeoPhoto) = viewModelScope.launch {
        repository.update(geoPhoto)
    }

    fun delete(geoPhoto: GeoPhoto) = viewModelScope.launch {
        repository.delete(geoPhoto)
    }
}

class GeoPhotoViewModelFactory(private val repository: GeoPhotoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeoPhotoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GeoPhotoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}