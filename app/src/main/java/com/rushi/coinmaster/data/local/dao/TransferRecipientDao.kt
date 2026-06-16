package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferRecipientDao {
    @Query("SELECT * FROM transfer_recipients WHERE is_deleted = 0 ORDER BY name ASC")
    fun getTransferRecipientsFlow(): Flow<List<TransferRecipientEntity>>

    @Query("SELECT * FROM transfer_recipients WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getTransferRecipients(): List<TransferRecipientEntity>

    @Query("SELECT * FROM transfer_recipients WHERE id = :id AND is_deleted = 0")
    suspend fun getTransferRecipientById(id: Long): TransferRecipientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferRecipient(recipient: TransferRecipientEntity): Long

    @Update
    suspend fun updateTransferRecipient(recipient: TransferRecipientEntity)

    @Query("UPDATE transfer_recipients SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteTransferRecipient(id: Long)
}
