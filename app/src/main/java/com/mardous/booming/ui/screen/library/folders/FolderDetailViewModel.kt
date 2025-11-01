package com.mardous.booming.ui.screen.library.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Christians M. A. (mardous)
 */
class FolderDetailViewModel(private val repository: Repository, private val folderPath: String) :
    ViewModel() {

    private val _folder = MutableLiveData<Folder>()
    fun getFolder(): LiveData<Folder> = _folder

    init {
        loadDetail()
    }

    fun loadDetail() = viewModelScope.launch(Dispatchers.IO) {
        _folder.postValue(repository.folderByPath(folderPath))
    }
}