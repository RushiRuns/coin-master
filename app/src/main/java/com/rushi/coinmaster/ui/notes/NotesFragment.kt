package com.rushi.coinmaster.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.NoteEntity
import com.rushi.coinmaster.databinding.FragmentNotesBinding
import com.rushi.coinmaster.databinding.ItemNoteBinding
import com.rushi.coinmaster.util.DateFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()

    private var editingNoteId: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSaveNote.setOnClickListener {
            saveNoteFromInput()
        }

        binding.btnCancel.setOnClickListener {
            resetFormState()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notesState.collectLatest { notes ->
                    renderNotesList(notes)
                }
            }
        }
    }

    private fun saveNoteFromInput() {
        val title = binding.etNoteTitle.text?.toString()?.trim() ?: ""
        val content = binding.etNoteContent.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            binding.tilNoteTitle.error = "Title cannot be empty"
            return
        }
        binding.tilNoteTitle.error = null

        if (content.isEmpty()) {
            binding.tilNoteContent.error = "Content cannot be empty"
            return
        }
        binding.tilNoteContent.error = null

        viewModel.saveNote(editingNoteId, title, content)
        
        Toast.makeText(
            requireContext(),
            if (editingNoteId == 0L) "Note created" else "Note updated",
            Toast.LENGTH_SHORT
        ).show()

        resetFormState()
    }

    private fun resetFormState() {
        editingNoteId = 0L
        binding.etNoteTitle.text = null
        binding.etNoteContent.text = null
        binding.tvFormTitle.text = "Create Note"
        binding.btnCancel.visibility = View.GONE
        binding.tilNoteTitle.error = null
        binding.tilNoteContent.error = null
    }

    private fun renderNotesList(notes: List<NoteEntity>) {
        binding.containerNotes.removeAllViews()
        
        if (notes.isEmpty()) {
            binding.tvEmptyNotes.visibility = View.VISIBLE
        } else {
            binding.tvEmptyNotes.visibility = View.GONE
            val languageCode = LocaleHelper.getLanguage(requireContext())

            for (note in notes) {
                val itemBinding = ItemNoteBinding.inflate(layoutInflater, binding.containerNotes, false)
                itemBinding.tvNoteTitle.text = note.title
                itemBinding.tvNoteContent.text = note.content
                itemBinding.tvNoteDate.text = "Last updated: ${DateFormatter.formatDate(note.updatedAt, languageCode)}"

                itemBinding.btnEdit.setOnClickListener {
                    editingNoteId = note.id
                    binding.etNoteTitle.setText(note.title)
                    binding.etNoteContent.setText(note.content)
                    binding.tvFormTitle.text = "Edit Note"
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.tilNoteTitle.error = null
                    binding.tilNoteContent.error = null
                    binding.etNoteTitle.requestFocus()
                }

                itemBinding.btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteNote(note.id)
                            Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
                            if (editingNoteId == note.id) {
                                resetFormState()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                binding.containerNotes.addView(itemBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
