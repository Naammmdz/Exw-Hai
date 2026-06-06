package com.example.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EsmeryServices
import com.example.data.EsmeryRepository
import com.example.data.EsmeryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseEsmeryViewModel(
  protected val repository: EsmeryRepository = EsmeryServices.repository,
) : ViewModel() {
  val esmeryState: StateFlow<EsmeryState> = repository.state

  private val _toast = MutableStateFlow<String?>(null)
  val toast: StateFlow<String?> = _toast.asStateFlow()

  protected fun showToast(message: String) {
    _toast.value = message
  }

  fun clearToast() {
    _toast.value = null
  }

  protected fun launchAction(block: suspend () -> Unit) {
    viewModelScope.launch {
      runCatching { block() }
        .onFailure { showToast(it.message ?: "Something went wrong.") }
    }
  }
}
