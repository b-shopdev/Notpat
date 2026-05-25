package com.example.data.local

import androidx.room.*
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE :query OR content LIKE :query) ORDER BY isPinned DESC, timestamp DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getUnsyncedNotes(): List<Note>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesDirect(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, isSynced = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteNote(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun hardDeleteSyncedDeletions()
}
