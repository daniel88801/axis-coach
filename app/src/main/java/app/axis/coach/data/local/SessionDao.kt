package app.axis.coach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(entity: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC LIMIT 1")
    fun observeLatest(): Flow<SessionEntity?>

    @Query("SELECT COUNT(*) FROM sessions")
    fun observeCount(): Flow<Int>
}
