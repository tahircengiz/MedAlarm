package com.medalarm.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medalarm.app.data.backup.JsonBackupRepository
import com.medalarm.app.pdf.PdfReportBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: BackupMessage? = null
)

enum class BackupMessage { SUCCESS, IMPORT_SUCCESS, ERROR }

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: JsonBackupRepository,
    private val pdfReportBuilder: PdfReportBuilder
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun exportJson(uri: Uri) = runOp(BackupMessage.SUCCESS) {
        backupRepository.export(uri)
    }

    fun importJson(uri: Uri, mode: JsonBackupRepository.ImportMode) =
        runOp(BackupMessage.IMPORT_SUCCESS) {
            backupRepository.import(uri, mode)
        }

    fun exportPdf(uri: Uri, days: Int = 30) = runOp(BackupMessage.SUCCESS) {
        pdfReportBuilder.export(uri, days)
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun runOp(success: BackupMessage, block: suspend () -> Result<Unit>) {
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                it.copy(
                    isBusy = false,
                    message = if (result.isSuccess) success else BackupMessage.ERROR
                )
            }
        }
    }
}
