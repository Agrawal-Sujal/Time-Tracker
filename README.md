# ⏱ Time Tracker — Android App

A sleek, dark-themed Android time tracking app built with **Jetpack Compose** + **Dagger Hilt** + **Room**.

---

## Features

- **Custom Tags** — Create colour-coded tags (DSA, CSF, Project, etc.)
- **Stopwatch Timer** — Start/stop sessions per tag
- **Persistent Foreground Service** — Timer keeps running when app is closed from recents
- **Live Notification** — Shows tag name + elapsed time with a Stop button
- **Analysis — Today** — Time spent per tag today with donut chart & bar chart
- **Analysis — This Week** — Weekly breakdown with percentages
- **Sorting** — Sort analysis by duration or name
- **Dark Blackish Theme** — OLED-friendly #080808 background

---

## Project Structure

```
app/src/main/java/com/timetracker/
├── data/
│   ├── local/
│   │   ├── dao/          # TagDao, TimeSessionDao
│   │   ├── entity/       # TagEntity, TimeSessionEntity
│   │   └── AppDatabase   # Room database
│   └── repository/       # TimeTrackerRepository
├── di/
│   └── AppModule         # Hilt DI — provides DB, DAOs
├── domain/
│   └── model/            # Tag, TimeSession, TagStats, ActiveSessionState
├── presentation/
│   ├── components/       # Shared Compose components
│   ├── navigation/       # Screen routes
│   ├── screens/
│   │   ├── home/         # HomeScreen — timer + tag picker
│   │   ├── analysis/     # AnalysisScreen — stats + charts
│   │   └── tags/         # TagsScreen — CRUD for tags
│   ├── theme/            # Dark color scheme
│   └── viewmodel/        # HomeViewModel, AnalysisViewModel
├── service/
│   └── TimerService      # Foreground service
├── util/                 # Duration formatters, color parser
├── MainActivity          # Bottom nav host
└── TimeTrackerApp        # @HiltAndroidApp
```

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35
- JDK 17

### Steps

1. **Open in Android Studio**
   ```
   File → Open → select the TimeTracker folder
   ```

2. **Sync Gradle**
   Android Studio will auto-sync. If not, click *Sync Project with Gradle Files*.

3. **Run on device / emulator**
   - Min SDK: 26 (Android 8.0)
   - Target SDK: 35

4. **Grant notification permission** when prompted (Android 13+).

---

## Architecture

```
UI (Compose Screens)
      ↕
ViewModels (Hilt, StateFlow)
      ↕
Repository (single source of truth)
      ↕
Room DAOs ──── SQLite DB
      ↕
TimerService (Foreground, LifecycleService)
```

- **Room** persists tags and time sessions
- **Hilt** injects the database, DAOs, and repository everywhere
- **StateFlow + collectAsState()** drives all UI reactivity
- **LifecycleService** (from `lifecycle-service`) ties the timer coroutine to the service lifecycle — no leaks
- **START_STICKY** ensures Android restarts the service if killed

---

## Notification Behaviour

| Event | Behaviour |
|-------|-----------|
| Timer started | Foreground notification appears immediately |
| App in background | Notification ticks every second |
| App removed from recents | Service restarts (START_STICKY), notification continues |
| Stop tapped in notification | Session saved, notification dismissed |

---

## Adding Default Tags

Tap **+** on the Home screen or go to the **Tags** tab. Pick a name and colour from the built-in palette (12 colours).

Suggested tags to create:
- 🔴 DSA
- 🔵 CSF  
- 🟢 Project
- 🟡 Revision
- 🟣 Reading
