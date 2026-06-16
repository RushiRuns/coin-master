package com.rushi.coinmaster.ui.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import com.rushi.coinmaster.data.repository.TransferRecipientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransfersViewModel @Inject constructor(
    private val transferRecipientRepository: TransferRecipientRepository
) : ViewModel() {

    val recipientsState: StateFlow<List<TransferRecipientEntity>> = transferRecipientRepository.getTransferRecipientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRecipient(id: Long, name: String, type: String, bankDetails: String?) {
        viewModelScope.launch {
            val recipient = TransferRecipientEntity(
                id = id,
                name = name,
                type = type,
                bankDetails = bankDetails
            )
            if (id == 0L) {
                transferRecipientRepository.insertTransferRecipient(recipient)
            } else {
                transferRecipientRepository.updateTransferRecipient(recipient)
            }
        }
    }

    fun deleteRecipient(id: Long) {
        viewModelScope.launch {
            transferRecipientRepository.softDeleteTransferRecipient(id)
        }
    }
}
