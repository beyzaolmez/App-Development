# Momentum

A gentle productivity app for students that turns daily tasks into manageable side quests. Earn XP, build streaks, and grow at your own pace without the pressure of traditional productivity tools.

## Features

### Authentication & Sessions
- **Registration** — Create account with email, password, and display name
- **Login** — Sign in with email and password
- **Session Persistence** — Firebase session survives app restarts; returning users land directly on Home
- **Sign Out** — Fully clears Firebase session token
- **Account Deletion** — Reauthenticates with the user's password, deletes private/user-authored Firestore data, ends shared streaks, and then deletes the Firebase Auth account
- **Firebase Auth** — Secure authentication with error handling and validation
- **Input Validation** — Real-time validation with user-friendly error messages

### Onboarding & Personalisation
- **3-Page Onboarding** — Swipeable intro shown on first launch (skippable)
- **Interest Selection** — New users pick quest categories after sign-up; stored locally via SharedPreferences
- **Quest Filtering** — Home screen shows only quests matching saved interests; falls back to all quests if none saved

### Quest System
- **Daily Quests** — 1-3 curated quests across Academic, Social, and Personal categories
- **Quest Status Tracking** — Available, Active, Skipped, and Completed states
- **Quest Cards** — Clean card UI with category chips, difficulty, and XP rewards
- **Quest Detail** — Full quest view with skip and save-for-later options
- **Completion Flow** — Mark quests done, records streak, add optional reflection

### Navigation & UI
- **Bottom Navigation** — Home, Reflect, Progress, Friends, Profile tabs
- **Jetpack Compose** — Modern declarative UI with Material Design 3
- **Dark Theme** — Brand-consistent dark-first design (#0B1326 background)
- **Custom Components** — Momentum-themed buttons, cards, chips, and text fields

### Progress & Notifications
- **Soft Streaks** — Consecutive-day streak tracked in SharedPreferences; resets on missed day
- **Live Streak Display** — Streak count shown on Home header chip and Progress stats card
- **Category Balance** — Visual breakdown of quest categories completed
- **Push Notifications** — Local quest reminder notifications with runtime permission request (Android 13+)

### Friends, Feedback & Data Sync
- **Shared Streaks** — Firestore-backed friend streaks with invite, accept, decline, cancel, and shared quest activity
- **Friend Quest Board** — Shows liked quests from active shared-streak friends
- **Quest Feedback** — Users can like or dislike quests; feedback is stored per user in Firestore
- **Quest Suggestions & App Feedback** — Profile actions submit suggestions and feedback to Firestore
- **Local Fallbacks** — Demo/local state keeps core flows usable when Firebase data is unavailable

## Tech Stack

- **Language:** Kotlin 1.9.0
- **UI Framework:** Jetpack Compose with Material3
- **Architecture:** MVVM (Model-View-ViewModel)
- **Navigation:** Jetpack Compose Navigation
- **Backend:** Firebase Authentication and Cloud Firestore
- **Local Storage:** SharedPreferences for interests, streaks, cached quest state, and theme preference
- **Notifications:** Android local notifications
- **Testing:** JUnit JVM unit tests
- **Build:** Gradle 8.13.2
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Documentation

- **Wireframe:** `docs/design/wireframe.html`
- **Firestore ERD:** `docs/architecture/momentum-firestore-erd-current.svg`

## Project Structure

```
docs/
├── architecture/
│   └── momentum-firestore-erd-current.svg # Current Firestore ERD
└── design/
    └── wireframe.html                     # Project wireframe

app/src/main/java/com/nhlstenden/momentum/
├── MomentumApplication.kt             # App entry point; registers notification channel
├── MainActivity.kt                    # Single activity; requests notification permission
├── data/
│   ├── repository/
│   │   ├── AuthRepository.kt          # Firebase Auth, profile sync, account deletion
│   │   ├── QuestRepository.kt         # Predefined quests + Firestore quest state
│   │   ├── SharedStreakRepository.kt  # Firestore shared streaks and invites
│   │   ├── QuestFeedbackRepository.kt # Firestore quest likes/dislikes
│   │   └── UserRepository.kt          # Firestore user profile/preferences
│   ├── InterestsStore.kt              # SharedPreferences — selected interest categories
│   ├── QuestLocalCache.kt             # SharedPreferences — offline quest cache
│   ├── SuggestionsStore.kt            # Firestore quest suggestions
│   └── FeedbackStore.kt               # Firestore app feedback
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
│   │   │   └── AuthHeader.kt          # Shared auth header
│   │   ├── onboarding/
│   │   │   ├── OnboardingScreen.kt    # 3-page swipeable first-run intro
│   │   │   └── InterestSelectionScreen.kt # Category chip picker after sign-up
│   │   ├── home/
│   │   │   └── HomeScreen.kt          # Daily quests filtered by interests + streak
│   │   ├── quest/
│   │   │   ├── QuestDetailScreen.kt   # Quest detail with skip / save-for-later
│   │   │   └── CompleteScreen.kt      # Completion screen; records streak
│   │   ├── reflect/
│   │   │   └── ReflectScreen.kt       # Optional reflection notes
│   │   ├── progress/
│   │   │   └── ProgressScreen.kt      # Streak stat + category balance bars
│   │   ├── friends/
│   │   │   ├── FriendsScreen.kt       # Friend streaks and invites
│   │   │   └── QuestBoardScreen.kt    # Friend-liked quest board
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
    ├── DailyQuestSelector.kt          # Pure daily quest selection/ranking rules
    ├── ProfileViewModel.kt            # Profile editing and account deletion state
    ├── QuestViewModel.kt              # Quest list/progress/feedback state
    └── SharedStreakViewModel.kt       # Shared streak invite and activity state
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
   The app uses Firebase Authentication. The required `app/google-services.json` file is already included for the default Firebase project. Only replace it if you want to use your own Firebase project:
   
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Create a new project or use existing
   - Add Android app with package name: `com.nhlstenden.momentum`
   - Download `google-services.json`
   - Place it in: `app/google-services.json`
   - Enable Email/Password authentication in Firebase Console → Authentication → Sign-in method

4. **Build and Run**
   - Connect device or start emulator
   - Click Run (▶) in Android Studio

5. **Run Unit Tests**
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Firebase Rules

Firestore security rules are stored in `firestore.rules`. They restrict private user data to the signed-in user, allow shared streak members to read/update their own shared streaks, allow invite senders to cancel pending invitations, and allow users to delete their own feedback/suggestions during account deletion.

### Manual QA Checklist

Before submitting or merging a feature branch, run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

Then verify the main app flows on an emulator or device:

- Register with email/password and complete onboarding interest selection
- Sign out and sign back in with the same account
- Load daily quests, start a quest, skip a quest, and complete a quest
- Add a reflection and confirm it appears in Reflect/Progress where applicable
- Edit profile display name and theme
- Send feedback and suggest a quest
- Send a test notification after granting notification permission
- Create, accept, decline, and cancel shared streak invites when Firebase test accounts are available
- Delete account with a wrong password and confirm a friendly error appears
- Delete account with the correct password and confirm Auth plus private/user-authored Firestore data are removed

## Architecture

### MVVM Pattern
- **View (Screen)** — Composable UI, observes ViewModel state
- **ViewModel** — Holds UI state, handles user actions, validates input
- **Repository** — Abstracts Firebase Auth operations

### State Management
- `StateFlow` for reactive UI updates
- Unidirectional data flow: UI → ViewModel → Repository → Firebase

### Navigation
- Single Activity with Compose Navigation
- Type-safe routes via `MomentumDestinations`
- Bottom nav for main screens, full-screen auth flow

### External Subsystems
- **Firebase Auth** handles email/password registration, login, profile display name updates, sign-out, and account deletion.
- **Cloud Firestore** stores user profiles, quest states, journal entries, shared streaks, quest feedback, app feedback, and quest suggestions.
- **SharedPreferences** stores lightweight local-only preferences and fallback state such as selected interests and streak metadata.
- **Android Notifications** provide local quest reminders through a registered notification channel.

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
