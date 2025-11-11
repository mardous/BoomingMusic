package com.mardous.booming.core.model.task

/**
 * Generic class that holds the network state
 */
sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}