package com.example.fittrack

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fittrack.data.Photo
import com.example.fittrack.data.PhotoDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val photoDao = PhotoDatabase.getDatabase(application).photoDao()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _photoDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val photoDates: StateFlow<List<LocalDate>> = _photoDates.asStateFlow()

    init {
        photoDao.getAllPhotos().onEach { photoList ->
            _photos.value = photoList
            _photoDates.value = photoList.map {
                Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }.launchIn(viewModelScope)
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            // Add current time when inserting a new photo
            photoDao.insert(Photo(uri = uri.toString(), createdAt = System.currentTimeMillis()))
        }
    }
}

class RecordViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
