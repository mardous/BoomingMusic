package com.mardous.booming.ui.screen.library.years

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.ReleaseYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YearDetailViewModel(private val repository: Repository, private val year: Int) : ViewModel() {

    private val _year = MutableLiveData<ReleaseYear>()
    fun getYear(): LiveData<ReleaseYear> = _year

    init {
        loadDetail()
    }

    fun loadDetail() = viewModelScope.launch(Dispatchers.IO) {
        _year.postValue(repository.yearById(year))
    }
}