package com.root14.detectionsdk.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.root14.detectionsdk.data.Events
import com.root14.detectionsdk.util.PermissionUtil
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _permissionGranted = MutableStateFlow<Boolean?>(null)
    val permissionGranted: Flow<Boolean?>
        get() = _permissionGranted.asStateFlow()

    fun grantPermission(context: Context) {
        viewModelScope.launch {
            flow {
                PermissionUtil.requestPermission(context)
                val isGranted = PermissionUtil.checkPermission(context)
                emit(isGranted)
            }.catch {
                _permissionGranted.value = false
            }.collect { isGranted ->
                if (!isGranted) {
                    grantPermission(context)
                }
            }
        }
    }

    private val _pushEventFlow = MutableStateFlow<Events?>(null)
    val pushEventFlow: StateFlow<Events?>
        get() = _pushEventFlow.asStateFlow()

    fun pushEvent(events: Events) {
        viewModelScope.launch {
            _pushEventFlow.emit(events)
        }
    }
}
