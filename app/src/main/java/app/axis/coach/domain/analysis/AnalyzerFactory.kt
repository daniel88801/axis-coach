package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.Exercise

object AnalyzerFactory {
    fun create(exercise: Exercise): ExerciseAnalyzer = when (exercise) {
        Exercise.SQUAT -> SquatAnalyzer()
        Exercise.PUSH_UP -> PushUpAnalyzer()
        Exercise.PLANK -> PlankAnalyzer()
    }
}
