package com.rushi.coinmaster.ui.notes

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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rushi.coinmaster.databinding.FragmentAddEditNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditNoteFragment : Fragment() {

    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private val args: AddEditNoteFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val isEditMode = args.noteId != 0L

        if (isEditMode) {
            binding.toolbar.title = "Edit Note"
            binding.btnSave.text = "Save Changes"
            viewModel.loadNote(args.noteId)

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.noteDetailState.collectLatest { note ->
                        if (note != null && binding.etNoteTitle.text.isNullOrEmpty() && binding.etNoteContent.text.isNullOrEmpty()) {
                            binding.etNoteTitle.setText(note.title)
                            binding.etNoteContent.setText(note.content)
                        }
                    }
                }
            }
        } else {
            binding.toolbar.title = "Create Note"
            binding.btnSave.text = "Save Note"
        }

        binding.btnSave.setOnClickListener {
            saveNote()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun saveNote() {
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

        viewModel.saveNote(args.noteId, title, content)

        Toast.makeText(
            requireContext(),
            if (args.noteId == 0L) "Note created" else "Note updated",
            Toast.LENGTH_SHORT
        ).show()

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
