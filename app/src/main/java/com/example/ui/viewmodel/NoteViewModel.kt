package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Note
import com.example.data.network.SyncResult
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NoteViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(database.noteDao())
    private val prefs: SharedPreferences = application.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE)

    // Search query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Screen navigation / Active Selected Note
    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    // Folder Category Filtering
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Server Sync URL configuration (stored persistently in Prefs)
    private val _serverUrl = MutableStateFlow(
        prefs.getString("sync_server_url", "https://placeholder-sync.notepad.com") ?: "https://placeholder-sync.notepad.com"
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    // Sync State variables
    private val _syncStatus = MutableStateFlow("Ready for cloud sync")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf(
            "[* SYSTEM READY] Offline SQLite Storage mounted.",
            "[* SYSTEM READY] Server Synchronization Protocol configured."
        )
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Main Notes list, combined dynamically with Search Query and Category Folder filter in Kotlin for absolute speed!
    val notes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        _searchQuery,
        _selectedCategory
    ) { allNotes, query, category ->
        var filteredList = allNotes

        // Apply folder category filter
        if (category != "All") {
            filteredList = filteredList.filter { it.category == category }
        }

        // Apply textual search filter
        if (query.isNotBlank()) {
            filteredList = filteredList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }

        filteredList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Stats calculations derived reactively
    val notesStats = notes.map { list ->
        val redCount = list.count { it.colorHex.equals("#FFFFCDD2", ignoreCase = true) || it.colorHex.equals("#FF5D1D21", ignoreCase = true) }
        val yellowCount = list.count { it.colorHex.equals("#FFF59D", ignoreCase = true) || it.colorHex.equals("#FF504615", ignoreCase = true) }
        val orangeCount = list.count { it.colorHex.equals("#FFFFCC80", ignoreCase = true) || it.colorHex.equals("#FF593E11", ignoreCase = true) }
        val creamCount = list.count { it.colorHex.equals("#FFE8F5E9", ignoreCase = true) || it.colorHex.equals("#FF17381B", ignoreCase = true) }
        val pinnedCount = list.count { it.isPinned }
        
        Stats(
            total = list.size,
            redNotes = redCount,
            yellowNotes = yellowCount,
            otherNotes = orangeCount + creamCount,
            pinnedNotes = pinnedCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Stats()
    )

    data class Stats(
        val total: Int = 0,
        val redNotes: Int = 0,
        val yellowNotes: Int = 0,
        val otherNotes: Int = 0,
        val pinnedNotes: Int = 0
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setActiveNote(note: Note?) {
        _activeNote.value = note
    }

    fun setServerUrl(url: String) {
        _serverUrl.value = url
        prefs.edit().putString("sync_server_url", url).apply()
        addLogLine("Custom Server Target updated: $url")
    }

    fun createNewNote() {
        // Create an empty, fresh temporary note structure and display detail screen
        setActiveNote(Note(title = "", content = "", isSynced = false))
    }

    fun saveActiveNote(title: String, content: String, colorHex: String, category: String, isPinned: Boolean) {
        val current = _activeNote.value ?: return
        viewModelScope.launch {
            val updatedNote = current.copy(
                title = title,
                content = content,
                colorHex = colorHex,
                category = category,
                isPinned = isPinned,
                timestamp = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis(),
                isSynced = false
            )

            repository.insertNote(updatedNote)
            _activeNote.value = null // Dismiss editor back to list overview
            addLogLine("Saved note locally: \"${if (title.isBlank()) "Untitled" else title}\". Marked unsynced.")
            
            // Proactive auto-syncing option! Let's auto sync to the cloud for maximum modern polish
            silentCloudSync()
        }
    }

    fun deleteNoteById(noteId: Int) {
        viewModelScope.launch {
            repository.softDeleteNote(noteId)
            _activeNote.value = null
            addLogLine("Sent Note ID $noteId to bin. Queued for cloud deletion sync.")
            
            // Auto-trigger sync to propagate the deletion
            silentCloudSync()
        }
    }

    fun forceWebSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncStatus.value = "Starting active cloud handshake..."
        
        viewModelScope.launch {
            addLogLine("HANDSHAKE: Polling local SQLite queue for changes...")
            val result = repository.performCloudSync(_serverUrl.value)
            
            when (result) {
                is SyncResult.Success -> {
                    _syncStatus.value = "Synced successfully!"
                    addLogLines(result.logs)
                    addLogLine("DONE: Database sync finalized. Ready.")
                }
                is SyncResult.Error -> {
                    _syncStatus.value = "Sync encountered warnings"
                    addLogLines(result.logs)
                    addLogLine("WARNING: Retry sync manually when connections stabilize.")
                }
            }
            _isSyncing.value = false
        }
    }

    private fun silentCloudSync() {
        viewModelScope.launch {
            // Quietly performs backend sync without blocking screen spinners
            repository.performCloudSync(_serverUrl.value)
        }
    }

    private fun addLogLine(line: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$time] $line"
        _syncLogs.value = _syncLogs.value + formatted
    }

    private fun addLogLines(lines: List<String>) {
        _syncLogs.value = _syncLogs.value + lines
    }

    fun clearLogConsole() {
        _syncLogs.value = listOf("[* TELEMETRY RESET] Console cleared at " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
    }
}
