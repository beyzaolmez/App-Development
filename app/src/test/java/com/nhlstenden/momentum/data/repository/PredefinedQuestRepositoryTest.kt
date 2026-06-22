package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.LongTermQuestProgress
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestDifficulty
import com.nhlstenden.momentum.data.model.QuestGoalType
import com.nhlstenden.momentum.data.model.QuestStatus
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredefinedQuestRepositoryTest {

    private val repository = PredefinedQuestRepository()

    @Test
    fun predefinedQuestsPassFirebaseContentValidation() {
        val validationIssues = QuestContentValidator.validateAll(repository.getQuests())

        assertTrue(validationIssues.joinToString(separator = "\n"), validationIssues.isEmpty())
    }

    @Test
    fun firebaseSeedPayloadMatchesQuestModelFields() {
        val expectedKeys = setOf(
            "title",
            "description",
            "category",
            "difficulty",
            "estimatedMinutes",
            "xp",
            "steps",
            "journalPrompt",
            "goalType",
            "targetProgress",
            "progressUnit",
            "isActive"
        )

        repository.getQuests().forEach { quest ->
            val payload = quest.toQuestSeedMap()

            assertEquals(expectedKeys, payload.keys)
            assertEquals(quest.title, payload["title"])
            assertEquals(quest.description, payload["description"])
            assertEquals(quest.category.name, payload["category"])
            assertEquals(quest.difficulty.name, payload["difficulty"])
            assertEquals(quest.estimatedMinutes, payload["estimatedMinutes"])
            assertEquals(quest.xp, payload["xp"])
            assertEquals(quest.steps, payload["steps"])
            assertEquals(quest.journalPrompt, payload["journalPrompt"])
            assertEquals(quest.goalType.name, payload["goalType"])
            assertEquals(quest.targetProgress, payload["targetProgress"])
            assertEquals(quest.progressUnit, payload["progressUnit"])
            assertEquals(true, payload["isActive"])
        }
    }

    @Test
    fun questSeedExporterProducesFirebaseQuestArtifact() {
        val seedJson = QuestSeedExporter.toJson(repository.getQuests())
        val outputPath = Paths.get(
            "build",
            "reports",
            "quest-seed",
            "firebase-quests.seed.json"
        )

        Files.createDirectories(outputPath.parent)
        Files.write(outputPath, seedJson.toByteArray())

        assertTrue(seedJson.startsWith("{\"collection\":\"quests\""))
        assertTrue(seedJson.contains("\"id\":\"workout-session\""))
        assertTrue(seedJson.contains("\"goalType\":\"LongTerm\""))
        assertTrue(seedJson.contains("\"progressUnit\":\"workouts\""))
        assertTrue(Files.exists(outputPath))
    }

    @Test
    fun validationRejectsIncompleteLongTermQuestFields() {
        val invalidQuest = repository.getQuestById("workout-streak")!!.copy(
            targetProgress = 1,
            progressUnit = "completion"
        )

        val validationIssues = QuestContentValidator.validate(invalidQuest)

        assertTrue(validationIssues.any { it.contains("targetProgress") })
        assertTrue(validationIssues.any { it.contains("progressUnit") })
    }

    @Test
    fun predefinedQuestsCoverAllSupportedCategories() {
        val categories = repository.getQuests().map { it.category }.toSet()

        assertEquals(QuestCategory.entries.toSet(), categories)
    }

    @Test
    fun predefinedQuestsIncludeWorkoutContent() {
        val workoutQuestIds = setOf(
            "workout-session",
            "bodyweight-circuit",
            "cardio-intervals",
            "core-reset",
            "workout-streak"
        )
        val workoutQuests = repository.getQuests().filter { it.id in workoutQuestIds }

        assertEquals(workoutQuestIds.size, workoutQuests.size)
        assertTrue(workoutQuests.any { it.id == "bodyweight-circuit" })
        assertTrue(workoutQuests.any { it.id == "cardio-intervals" })
        assertTrue(workoutQuests.any { it.id == "core-reset" })
        assertTrue(workoutQuests.any { it.id == "workout-streak" })
    }

    @Test
    fun workoutQuestsAppearInMovementCategory() {
        val workoutQuestIds = setOf(
            "workout-session",
            "bodyweight-circuit",
            "cardio-intervals",
            "core-reset",
            "workout-streak"
        )

        val workoutQuests = repository.getQuests().filter { it.id in workoutQuestIds }

        assertEquals(workoutQuestIds.size, workoutQuests.size)
        assertTrue(workoutQuests.all { it.category == QuestCategory.Movement })
    }

    @Test
    fun predefinedQuestEnumsAreRepresentedAsFirebaseNames() {
        val quest = repository.getQuestById("chapter-focus")!!
        val payload = quest.toQuestSeedMap()

        assertEquals(QuestCategory.Academic.name, payload["category"])
        assertEquals(QuestDifficulty.Medium.name, payload["difficulty"])
        assertEquals(QuestGoalType.Daily.name, payload["goalType"])
    }

    @Test
    fun dailyWorkoutQuestsCanBeCompletedManually() {
        val dailyWorkoutQuests = repository.getQuests()
            .filter { it.category == QuestCategory.Movement && it.id != "workout-streak" }
            .filter { it.goalType == QuestGoalType.Daily }

        assertFalse(dailyWorkoutQuests.isEmpty())
        dailyWorkoutQuests.forEach { quest ->
            val completedQuest = quest.copy(status = QuestStatus.Completed)

            assertEquals(QuestStatus.Completed, completedQuest.status)
            assertTrue(quest.steps.isNotEmpty())
            assertTrue(quest.xp > 0)
            assertTrue(quest.targetProgress >= 1)
        }
    }

    @Test
    fun longTermWorkoutQuestCanBeProgressedToCompletion() {
        val workoutStreak = repository.getQuestById("workout-streak")

        assertNotNull(workoutStreak)
        assertEquals(QuestGoalType.LongTerm, workoutStreak!!.goalType)
        assertEquals(4, workoutStreak.targetProgress)
        assertEquals("workouts", workoutStreak.progressUnit)

        val almostComplete = workoutStreak.copy(
            status = QuestStatus.Active,
            currentProgress = 3
        )
        val update = LongTermQuestProgress.updatedQuest(
            quest = almostComplete,
            progressDelta = 1,
            now = 10L
        )

        assertEquals(4, update.quest.currentProgress)
        assertEquals(QuestStatus.Completed, update.quest.status)
        assertTrue(update.completedNow)
    }
}
