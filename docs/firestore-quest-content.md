# Firestore Quest Content

This document records the production quest content model used by the app and the
expected Firestore shape for seeded quest records.

## ERD

```text
users/{uid}
  uid: string
  displayName: string
  email: string
  progress: map

users/{uid}/questStates/{questStateId}
  questStateId: string
  questId: string -> quests/{questId}
  date: string (yyyy-MM-dd)
  status: Available | Active | Completed | Skipped
  isDailyAssigned: boolean
  startedAt: number?
  completedAt: number?
  skippedAt: number?
  currentProgress: number
  targetProgress: number
  progressUnit: string
  lastProgressUpdatedAt: number?

quests/{questId}
  title: string
  description: string
  category: Academic | Focus | Wellbeing | Social | Movement
  difficulty: Easy | Medium | Hard
  estimatedMinutes: number
  xp: number
  steps: string[]
  journalPrompt: string?
  goalType: Daily | LongTerm
  targetProgress: number
  progressUnit: string
  isActive: boolean
```

## Validation Rules

Quest content is validated by `QuestContentValidator` before predefined content
is used for demo fallback and before any seed payload is written.

Required content checks:

- Every quest has a non-blank id, title, description, and at least one step.
- XP is positive and estimated minutes are never negative.
- Every supported category has at least one quest.
- Workout quests exist in Firebase seed content:
  `workout-session`, `bodyweight-circuit`, `cardio-intervals`, `core-reset`,
  and `workout-streak`.
- Workout quests are in the `Movement` category.
- Long-term quests use `goalType = LongTerm`, `targetProgress > 1`, and a
  specific `progressUnit` such as `workouts`, `sessions`, `entries`, or `days`.
- Seed payloads use enum `name` values, matching the app model and Firestore
  parser.

## Seeding

The mobile client must not be the trusted source for production quest writes.
`firestore.rules` intentionally allows signed-in users to read `/quests` but
blocks client writes:

```text
match /quests/{questId} {
  allow get, list: if signedIn();
  allow write: if false;
}
```

Production quest records should therefore be seeded through a trusted admin
context, Firebase Console import, or another privileged backend process using
the payload produced by `Quest.toQuestSeedMap()`.

The JVM test suite also generates a seed artifact at:

```text
app/build/reports/quest-seed/firebase-quests.seed.json
```

Generate or refresh it with:

```powershell
.\gradlew.bat testDebugUnitTest
```

Seed it with the admin utility:

```powershell
npm install
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\firebase-service-account.json"
npm run seed:quests:dry-run
npm run seed:quests -- --write
```

The script uses Application Default Credentials through the Firebase Admin SDK.
It defaults to dry-run mode and only writes when `--write` is passed.

If you need to target another Firebase project, set:

```powershell
$env:FIREBASE_PROJECT_ID="your-project-id"
```

The app still handles an empty or partial remote catalog gracefully by falling
back to predefined quests locally. If Firebase contains malformed active quest
documents, the repository skips those documents instead of defaulting them into
the wrong category or difficulty.

## Verification

Run the JVM tests before changing quest content:

```powershell
.\gradlew.bat testDebugUnitTest
```

Relevant coverage lives in
`app/src/test/java/com/nhlstenden/momentum/data/repository/PredefinedQuestRepositoryTest.kt`.
