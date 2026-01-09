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
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val photoDao = PhotoDatabase.getDatabase(application).photoDao()

    // 1. Private MutableStateFlow for internal updates
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    // 2. Public read-only StateFlow for the UI
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    // 3. Load data when ViewModel is created
    init {
        viewModelScope.launch {
            photoDao.getAllPhotos().collect { photoList ->
                _photos.value = photoList
            }
        }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            photoDao.insert(Photo(uri = uri.toString()))
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
