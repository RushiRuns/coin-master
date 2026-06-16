package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.TransferRecipientDao
import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRecipientRepository @Inject constructor(
    private val transferRecipientDao: TransferRecipientDao
) {
    fun getTransferRecipientsFlow(): Flow<List<TransferRecipientEntity>> = transferRecipientDao.getTransferRecipientsFlow()
    
    suspend fun getTransferRecipients(): List<TransferRecipientEntity> = transferRecipientDao.getTransferRecipients()

    suspend fun getTransferRecipientById(id: Long): TransferRecipientEntity? = transferRecipientDao.getTransferRecipientById(id)

    suspend fun insertTransferRecipient(recipient: TransferRecipientEntity): Long = transferRecipientDao.insertTransferRecipient(recipient)

    suspend fun updateTransferRecipient(recipient: TransferRecipientEntity) = transferRecipientDao.updateTransferRecipient(recipient)

    suspend fun softDeleteTransferRecipient(id: Long) = transferRecipientDao.softDeleteTransferRecipient(id)
}
