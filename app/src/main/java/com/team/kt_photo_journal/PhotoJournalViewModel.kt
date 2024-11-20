package com.team.kt_photo_journal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.team.kt_photo_journal.model.GeoPhoto
import com.team.kt_photo_journal.model.GeoPhotoRepository
import kotlinx.coroutines.launch

class PhotoJournalViewModel(private val repository: GeoPhotoRepository) : ViewModel() {

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allGeoPhotos: LiveData<Map<Int, GeoPhoto>> = repository.allGeoPhotos.asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(geoPhoto: GeoPhoto) = viewModelScope.launch {
        repository.insert(geoPhoto)
    }

    fun delete(geoPhoto: GeoPhoto) = viewModelScope.launch {
        repository.delete(geoPhoto)
    }

    fun update(geoPhoto: GeoPhoto) = viewModelScope.launch {
        repository.update(geoPhoto)
    }
}

class PhotoJournalViewModelFactory(private val repository: GeoPhotoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoJournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoJournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}