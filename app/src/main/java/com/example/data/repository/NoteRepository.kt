package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.Note
import com.example.data.network.SyncResult
import com.example.data.network.SyncService
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val syncService: SyncService = SyncService()
) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            allNotes
        } else {
            noteDao.searchNotes("%$query%")
        }
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note) {
        // Automatically touch modification fields and reset sync state
        val touchNote = note.copy(
            isSynced = false,
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        noteDao.insertNote(touchNote)
    }

    suspend fun softDeleteNote(noteId: Int) {
        noteDao.softDeleteNote(noteId, System.currentTimeMillis())
    }

    suspend fun hardDeleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun performCloudSync(serverUrl: String): SyncResult {
        // Collect all local notes that need cloud push:
        // 1. Any note that is not synced yet (including softDeleted ones so the server knows they are gone!)
        val unsyncedNotes = noteDao.getUnsyncedNotes()

        val result = syncService.syncNotes(unsyncedNotes, serverUrl)

        if (result is SyncResult.Success) {
            // Mark successfully synced notes as isSynced = true
            unsyncedNotes.forEach { note ->
                if (!note.isDeleted) {
                    val syncedNote = note.copy(isSynced = true)
                    noteDao.insertNote(syncedNote)
                }
            }
            // Now, physically delete from local SQLite database any notes that are soft-deleted and successfully synced!
            noteDao.hardDeleteSyncedDeletions()
        }

        return result
    }
}
