package com.nhlstenden.momentum.data.model

/**
 * Pure, side-effect-free rules for quest feedback actions.
 *
 * Kept separate from the ViewModel/repository so the "like" logic can be unit
 * tested without Firebase or Android dependencies.
 */
object QuestFeedbackRules {

    /**
     * A like is a duplicate when the quest is already liked. Duplicate likes are
     * rejected so the same quest can never be liked twice.
     */
    fun isDuplicateLike(existing: QuestFeedbackType?, incoming: QuestFeedbackType): Boolean =
        incoming == QuestFeedbackType.Like && existing == QuestFeedbackType.Like

    /**
     * A dislike is a duplicate when the quest is already disliked. Duplicate
     * dislikes are rejected so the same quest can never be disliked twice.
     */
    fun isDuplicateDislike(existing: QuestFeedbackType?, incoming: QuestFeedbackType): Boolean =
        incoming == QuestFeedbackType.Dislike && existing == QuestFeedbackType.Dislike
}