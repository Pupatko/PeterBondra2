package com.example.peterbondra

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY id DESC")
    fun observeTodoTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 1 ORDER BY id DESC")
    fun observeDoneTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 0")
    suspend fun getTodoTasksOnce(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Query("UPDATE tasks SET isDone = 1 WHERE id = :taskId")
    suspend fun markDone(taskId: Long)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
}
