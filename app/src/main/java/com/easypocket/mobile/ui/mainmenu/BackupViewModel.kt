package com.easypocket.mobile.ui.mainmenu

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easypocket.mobile.data.backup.BackupData
import com.easypocket.mobile.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _pendingImport = MutableStateFlow<BackupData?>(null)
    val pendingImport: StateFlow<BackupData?> = _pendingImport.asStateFlow()

    fun export(uri: Uri, contentResolver: ContentResolver, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(backupManager.exportTo(uri, contentResolver))
        }
    }

    fun prepareImport(uri: Uri, contentResolver: ContentResolver, onParsed: (Result<BackupData>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.parse(uri, contentResolver)
            if (result.isSuccess) _pendingImport.value = result.getOrNull()
            onParsed(result)
        }
    }

    fun confirmImport(onDone: (Result<Unit>) -> Unit) {
        val backup = _pendingImport.value
        if (backup == null) {
            onDone(Result.failure(IllegalStateException("No pending import")))
            return
        }
        viewModelScope.launch {
            try {
                backupManager.importData(backup)
                _pendingImport.value = null
                onDone(Result.success(Unit))
            } catch (t: Throwable) {
                onDone(Result.failure(t))
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }
}
