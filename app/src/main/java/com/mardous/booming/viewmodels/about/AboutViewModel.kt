package com.mardous.booming.viewmodels.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.BuildConfig
import com.mardous.booming.model.about.Contribution
import com.mardous.booming.repository.Repository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel(private val repository: Repository) : ViewModel() {

    private val _contributors = MutableStateFlow(emptyList<Contribution>())
    val contributors = _contributors.asStateFlow()

    private val _translators = MutableStateFlow(emptyList<Contribution>())
    val translators = _translators.asStateFlow()

    val appVersion: String
        get() = BuildConfig.VERSION_NAME

    fun loadContributors() = viewModelScope.launch(IO) {
        _contributors.value = repository.contributors()
    }

    fun loadTranslators() = viewModelScope.launch(IO) {
        _translators.value = repository.translators()
    }
}