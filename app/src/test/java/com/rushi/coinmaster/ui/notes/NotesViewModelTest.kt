package com.rushi.coinmaster.ui.notes

import com.rushi.coinmaster.data.local.entity.NoteEntity
import com.rushi.coinmaster.data.repository.NoteRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val noteRepository: NoteRepository = mockk(relaxed = true)
    private lateinit var viewModel: NotesViewModel

    private val testNotes = listOf(
        NoteEntity(id = 1L, title = "Note 1", content = "Content 1", updatedAt = 1000L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { noteRepository.getNotesFlow() } returns flowOf(testNotes)
        viewModel = NotesViewModel(noteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNotesStateExposesRepositoryFlow() = runTest {
        val notesList = mutableListOf<List<NoteEntity>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notesState.collect {
                notesList.add(it)
            }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(testNotes, viewModel.notesState.value)
        assertEquals(testNotes, notesList.last())
    }

    @Test
    fun testSaveNewNoteInsertsInRepository() = runTest {
        viewModel.saveNote(0L, "New Title", "New Content")
        testScheduler.advanceUntilIdle()
        coVerify { noteRepository.insertNote(match {
            it.id == 0L && it.title == "New Title" && it.content == "New Content"
        }) }
    }

    @Test
    fun testSaveExistingNoteUpdatesInRepository() = runTest {
        viewModel.saveNote(1L, "Updated Title", "Updated Content")
        testScheduler.advanceUntilIdle()
        coVerify { noteRepository.updateNote(match {
            it.id == 1L && it.title == "Updated Title" && it.content == "Updated Content"
        }) }
    }

    @Test
    fun testDeleteNoteDeletesInRepository() = runTest {
        viewModel.deleteNote(1L)
        testScheduler.advanceUntilIdle()
        coVerify { noteRepository.softDeleteNote(1L) }
    }

    @Test
    fun testLoadNote() = runTest {
        val expectedNote = NoteEntity(id = 5L, title = "Title 5", content = "Content 5", updatedAt = 5000L)
        coEvery { noteRepository.getNoteById(5L) } returns expectedNote

        viewModel.loadNote(5L)
        testScheduler.advanceUntilIdle()

        assertEquals(expectedNote, viewModel.noteDetailState.value)
    }
}
