package com.bolao.copa2026.domain.model

/**
 * Defines the point values for each scoring category in a group.
 * All values must be in the range [0, 999].
 */
data class ScoringSystem(
    val exactScore: Int,
    val correctWinnerAndWinnerGoals: Int,
    val correctWinnerAndLoserGoals: Int,
    val correctDraw: Int,
    val correctWinner: Int
) {
    init {
        require(exactScore in 0..999) {
            "exactScore must be in 0..999, was $exactScore"
        }
        require(correctWinnerAndWinnerGoals in 0..999) {
            "correctWinnerAndWinnerGoals must be in 0..999, was $correctWinnerAndWinnerGoals"
        }
        require(correctWinnerAndLoserGoals in 0..999) {
            "correctWinnerAndLoserGoals must be in 0..999, was $correctWinnerAndLoserGoals"
        }
        require(correctDraw in 0..999) {
            "correctDraw must be in 0..999, was $correctDraw"
        }
        require(correctWinner in 0..999) {
            "correctWinner must be in 0..999, was $correctWinner"
        }
    }
}
