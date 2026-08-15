/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.model.Folder
import com.mardous.booming.data.repository.Repository
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