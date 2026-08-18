package app.axis.coach.data.repo

import app.axis.coach.data.local.SessionDao
import app.axis.coach.data.local.SessionEntity
import app.axis.coach.domain.model.Exercise
import app.axis.coach.domain.model.FinishedSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao,
) {
    fun observeSessions(): Flow<List<FinishedSession>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    fun observeLatest(): Flow<FinishedSession?> = dao.observeLatest().map { it?.toDomain() }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun get(id: Long): FinishedSession? = dao.getById(id)?.toDomain()

    suspend fun save(
        exercise: Exercise,
        startedAt: Long,
        endedAt: Long,
        reps: Int,
        holdSeconds: Int,
        formScore: Int,
        topCue: String?,
    ): Long {
        return dao.insert(
            SessionEntity(
                exerciseId = exercise.id,
                startedAt = startedAt,
                endedAt = endedAt,
                durationMs = (endedAt - startedAt).coerceAtLeast(0),
                reps = reps,
                holdSeconds = holdSeconds,
                formScore = formScore,
                topCue = topCue,
            ),
        )
    }
}

private fun SessionEntity.toDomain() = FinishedSession(
    id = id,
    exercise = Exercise.fromId(exerciseId),
    startedAt = startedAt,
    endedAt = endedAt,
    durationMs = durationMs,
    reps = reps,
    holdSeconds = holdSeconds,
    formScore = formScore,
    topCue = topCue,
)
