package com.example.fittrack

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fittrack.data.FitTrackDatabase
import com.example.fittrack.data.Photo
import com.example.fittrack.data.PhotoDatabase
import com.example.fittrack.data.TodayExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val photoDao = PhotoDatabase.getDatabase(application).photoDao()
    private val exerciseDao = FitTrackDatabase.getInstance(application).todayExerciseDao()

    private val _photoDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val photoDates: StateFlow<List<LocalDate>> = _photoDates.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val exercisesForSelectedDate: StateFlow<List<TodayExerciseEntity>> =
        combine(selectedDate, photoDates) { date, photoDates ->
            if (photoDates.contains(date)) {
                date
            } else {
                null
            }
        }.flatMapLatest { date ->
            date?.let {
                exerciseDao.observeToday(it.toString())
            } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val photosForSelectedDate: StateFlow<List<Photo>> = selectedDate.flatMapLatest { date ->
        photoDao.getPhotoForDate(date.toString())
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())


    val markedDates: StateFlow<List<LocalDate>> = photoDates

    init {
        photoDao.getAllPhotos().onEach { photoList ->
            _photoDates.value = photoList.mapNotNull {
                try {
                    LocalDate.parse(it.date)
                } catch (e: DateTimeParseException) {
                    null
                }
            }.distinct()
        }.launchIn(viewModelScope)
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addPhoto(uri: Uri?) {
        viewModelScope.launch {
            val imageUri = uri ?: Uri.parse("android.resource://com.example.fittrack/drawable/dumbel")
            val photo = Photo(
                uri = imageUri.toString(),
                date = _selectedDate.value.toString(),
                createdAt = System.currentTimeMillis()
            )
            photoDao.insert(photo)
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            photoDao.delete(photo)
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
