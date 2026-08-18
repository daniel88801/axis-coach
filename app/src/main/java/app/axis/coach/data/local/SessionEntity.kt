package app.axis.coach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val reps: Int,
    val holdSeconds: Int,
    val formScore: Int,
    val topCue: String?,
)
