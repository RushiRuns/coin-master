package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.NoteDao
import com.rushi.coinmaster.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getNotesFlow(): Flow<List<NoteEntity>> = noteDao.getNotesFlow()
    
    suspend fun getNotes(): List<NoteEntity> = noteDao.getNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun softDeleteNote(id: Long) = noteDao.softDeleteNote(id)
}
