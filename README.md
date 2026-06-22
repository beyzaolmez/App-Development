# Momentum

A gentle productivity app for students that turns daily tasks into manageable side quests. Earn XP, build streaks, and grow at your own pace without the pressure of traditional productivity tools.

## Features

### Authentication & Sessions
- **Registration** — Create account with email, password, and display name
- **Login** — Sign in with email and password
- **Session Persistence** — Firebase session survives app restarts; returning users land directly on Home
- **Sign Out** — Fully clears Firebase session token
- **Firebase Auth** — Secure authentication with error handling and validation
- **Input Validation** — Real-time validation with user-friendly error messages

### Onboarding & Personalisation
- **3-Page Onboarding** — Swipeable intro shown on first launch (skippable)
- **Interest Selection** — New users pick quest categories after sign-up; stored locally via SharedPreferences
- **Quest Filtering** — Home screen shows only quests matching saved interests; falls back to all quests if none saved

### Quest System
- **Daily Quests** — Up to 3 curated daily quests across Academic, Focus, Wellbeing, Social, and Movement categories
- **Long-Term Quests** — Multi-step goals track progress over several days or sessions
- **Quest Status Tracking** — Available, Active, Skipped, and Completed states
- **Quest Cards** — Clean card UI with category chips, difficulty, and XP rewards
- **Quest Detail** — Full quest view with start, skip, completion, feedback, and progress actions
- **Completion Flow** — Mark quests done, records streaks, and supports quest reflections

### Navigation & UI
- **Bottom Navigation** — Home, Reflect, Progress, Friends, Profile tabs
- **Jetpack Compose** — Modern declarative UI with Material Design 3
- **Dark Theme** — Brand-consistent dark-first design (#0B1326 background)
- **Custom Components** — Momentum-themed buttons, cards, chips, and text fields

### Progress & Notifications
- **Soft Streaks** — Consecutive-day streak tracked in user progress and cached locally for resilience
- **Live Streak Display** — Streak count shown on Home header chip and Progress stats card
- **Category Balance** — Visual breakdown of quest categories completed
- **Shared Streaks** — Invite friends and track paired daily completion streaks
- **Push Notifications** — Local quest reminder notifications with runtime permission request (Android 13+)

## Tech Stack

- **Language:** Kotlin 1.9.0
- **UI Framework:** Jetpack Compose with Material3
- **Architecture:** MVVM (Model-View-ViewModel)
- **Navigation:** Jetpack Compose Navigation
- **Backend:** Firebase Authentication and Cloud Firestore
- **Build:** Gradle 8.13.2
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Project Structure

```
app/src/main/java/com/nhlstenden/momentum/
├── MomentumApplication.kt             # App entry point; registers notification channel
├── MainActivity.kt                    # Single activity; requests notification permission
├── data/
│   ├── repository/
│   │   ├── AuthRepository.kt          # Firebase Auth (register, login, signOut, currentUser)
│   │   ├── QuestRepository.kt         # Predefined quests, Firestore quest loading, quest state sync
│   │   ├── ReflectionRepository.kt    # Firestore-backed quest reflections
│   │   ├── SharedStreakRepository.kt  # Friend/shared streak persistence
│   │   └── UserRepository.kt          # User profile and progress persistence
│   ├── InterestsStore.kt              # SharedPreferences — selected interest categories
│   ├── QuestLocalCache.kt             # Local quest state/user progress cache
│   └── StreakStore.kt                 # Legacy local streak helper
├── navigation/
│   ├── MomentumDestinations.kt        # Route constants + bottom tab definitions
│   └── MomentumNavGraph.kt            # Nav graph; session-aware start destination
├── notification/
│   └── NotificationHelper.kt          # Local push notifications (channel + send)
├── ui/
│   ├── components/
│   │   ├── Buttons.kt                 # Primary, secondary, quiet buttons
│   │   ├── Cards.kt                   # Quest cards and generic cards
│   │   ├── Chips.kt                   # Category, skills, reward chips
│   │   ├── TextFields.kt              # Input fields with validation
│   │   └── BottomNav.kt               # Bottom navigation bar
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── WelcomeScreen.kt       # Landing screen
│   │   │   ├── SignInScreen.kt        # Login with Firebase
│   │   │   ├── SignUpScreen.kt        # Registration with validation
│   │   │   ├── ForgotPasswordScreen.kt # Password reset
│   │   │   └── AuthHeader.kt          # Shared auth header
│   │   ├── onboarding/
│   │   │   ├── OnboardingScreen.kt    # 3-page swipeable first-run intro
│   │   │   └── InterestSelectionScreen.kt # Category chip picker after sign-up
│   │   ├── home/
│   │   │   └── HomeScreen.kt          # Daily and long-term quests filtered by interests
│   │   ├── quest/
│   │   │   ├── QuestDetailScreen.kt   # Quest detail with skip, feedback, completion, and progress actions
│   │   │   ├── QuestReflectionScreen.kt # Quest-linked reflection capture
│   │   │   └── CompleteScreen.kt      # Completion confirmation screen
│   │   ├── reflect/
│   │   │   └── ReflectScreen.kt       # Optional reflection notes
│   │   ├── progress/
│   │   │   └── ProgressScreen.kt      # Streak stats, long-term progress, and category balance
│   │   ├── friends/
│   │   │   └── FriendsScreen.kt       # Friend streaks and invites
│   │   └── profile/
│   │       └── ProfileScreen.kt       # Real user name/email, interests, sign out
│   └── theme/
│       ├── Color.kt                   # Brand colors
│       ├── Theme.kt                   # MomentumTheme
│       ├── Type.kt                    # Typography (Space Grotesk + Lexend)
│       ├── Shape.kt                   # Corner radius definitions
│       └── Spacing.kt                 # 8-point grid spacing
└── viewmodel/
    ├── AuthViewModel.kt               # Auth state, validation, login/register logic
    └── QuestViewModel.kt              # Quest loading, status changes, progress, feedback, and reflections
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API 34
- JDK 17

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/beyzaolmez/App-Development.git
   cd App-Development
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync to complete

3. **Firebase Setup (Required for Auth)**
   The app uses Firebase Authentication. You need to add your own `google-services.json`:
   
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Create a new project or use existing
   - Add Android app with package name: `com.nhlstenden.momentum`
   - Download `google-services.json`
   - Place it in: `app/google-services.json`
   - Enable Email/Password authentication in Firebase Console → Authentication → Sign-in method
   - Quest collection schema, validation, and seeding notes are documented in `docs/firestore-quest-content.md`
   - Production quest seeding is handled by the Firebase Admin SDK script in `scripts/seed-firestore-quests.mjs`

4. **Build and Run**
   - Connect device or start emulator
   - Click Run (▶) in Android Studio

## Architecture

### MVVM Pattern
- **View (Screen)** — Composable UI, observes ViewModel state
- **ViewModel** — Holds UI state, handles user actions, validates input
- **Repository** — Abstracts Firebase Auth, Firestore data, local fallbacks, and cache operations

### State Management
- Compose state and ViewModels for reactive UI updates
- Unidirectional data flow: UI → ViewModel → Repository → Firebase

### Navigation
- Single Activity with Compose Navigation
- Type-safe routes via `MomentumDestinations`
- Bottom nav for main screens, full-screen auth flow

## Brand Guidelines

- **Colors:** Dark-first palette (#0B1326 background, #C0C1FF primary, #4CD7F6 secondary)
- **Typography:** Space Grotesk (headlines) + Lexend (body)
- **Shape:** 12dp radius for cards, pill shape for chips
- **Spacing:** 8-point base grid

## Git Workflow

- `main` — Production-ready code
- `develop` — Integration branch for features
- `feature/*` — Individual feature branches (e.g., `feature/registration`, `feature/login`)

## Contributing

1. Create feature branch from `develop`
2. Implement changes with clear commit messages
3. Push branch and create Pull Request to `develop`
4. Code review and merge

## License

This is an academic project for NHL Stenden University.
