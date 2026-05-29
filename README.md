# 🌌 Midlu's Routine

🚀 **Midlu's Routine** is a highly interactive, cosmic-themed habit and routine management application built with modern Android practices. Melding the clean, structured aesthetic of **Bento Design** with a stellar ambient color palette, the app empowers users to seamlessly synchronize their terrestrial routines in spacetime across device sessions.

---

## 🛰️ System Dashboard & Status

```
   ┌─────────────────────────────────────────────────────────┐
   │                  MIDLU'S ROUTINE CORE                  │
   ├───────────────┬───────────────────────┬─────────────────┤
   │ Local Room DB │   Google Auth Conduit │  Cloud Firestore│
   │   [Active]    │     [Interactive]     │    [Synced]     │
   └───────────────┴───────────────────────┴─────────────────┘
```

![Android](https://img.shields.io/badge/Platform-Android_12%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack-Compose_Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth_%26_Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

---

## 🎨 Architectural & Visual Masterpieces

### 🌀 1. Bento Space Aesthetic
* **Dynamic Grid Layouts:** Leverages nested modern Material 3 cards mimicking premium physical modules.
* **Ambient Themes:** Seamless toggle between deep **Space Charcoal Slate** and pristine **Stellar Nebula Light** modes.
* **Micro-interactions:** Delightful custom touch feedback, smooth expand/collapse states, and high-density typography pairings using robust sans-serif fonts.

### 🛡️ 2. Google Auth & Dev Sandbox Conduit
* Fully handles integrated authentications and transitions smoothly.
* **Google Play Sandbox Fallback:** Automatically detects if Google Play Services are absent from virtual test environments and fires up the *Cosmic Google Auth Conduit*, allowing instant sandbox account simulations (`nibirbhuiyan18@gmail.com`) to skip setup delays.

### 💾 3. Room Offline-First Engine & Firestore Sync
* **Local Persistence:** Powered by SQLite running through Kotlin Flow-backed Room DAOs.
* **Realtime Cloud Synchronization:** Once authenticated, completions, active streaks, personalized bios, custom titles, and schedules are pushed live to Firebase Firestore.
* **Dirty Cache Protection:** Automatically clears local caches and dynamic data when logging out to keep personal routines isolated and secure.

---

## 🗺️ Architectural Structure

```
                  ┌──────────────────────────────────────────────┐
                  │              Jetpack Compose UI              │
                  │   (AuthScreen, HabitScreens, Bento Elements) │
                  └───────────────────────┬──────────────────────┘
                                          │ Observes Flow
                                          ▼
                  ┌──────────────────────────────────────────────┐
                  │              RoutineViewModel                │
                  │         Manages user profiles & UI state     │
                  └───────────────────────┬──────────────────────┘
                                          │ Writes/Reads
                     ┌────────────────────┴────────────────────┐
                     ▼                                         ▼
         ┌───────────────────────┐                 ┌───────────────────────┐
         │     Local Engine      │                 │     Cloud Conduit     │
         │  (Room AppDatabase)   │                 │ (FirebaseSyncHelper)  │
         │  Habits & Completions │                 │    Google Auth SSO    │
         └───────────────────────┘                 └───────────────────────┘
```

---

## 🛠️ Technological Arsenal

* **Language:** `Kotlin 1.9` - Multi-paradigm, expressive, and safe.
* **UI Engine:** `Jetpack Compose 1.5` - Declarative UI utilizing robust design configurations.
* **Local Database:** `Room SQLite` - Reactive queries yielding Kotlin Flows.
* **Remote Database:** `Cloud Firestore` - Document-based cloud synchronization.
* **JSON Serialization:** `kotlinx.serialization` - Type-safe compile-time translation.
* **Asynchronous Flow:** `Kotlin Coroutines & Flow` - Concurrency without blocking system threads.

---

## 🕹️ Getting Started & Launch Guidelines

### Prerequisite Setup
Ensure your local or remote workspace includes an configured active `.env` file containing firebase keys or place them through your Google Cloud Console Secrets if accessing remote APIs:
```bash
# Example local build environment configurations
FIREBASE_API_KEY=your_cosmic_firebase_key
FIREBASE_PROJECT_ID=your_project_id
```

### Installation
1. Clone the galaxy repository locally:
   ```bash
   git clone https://github.com/your-username/midlus-routine.git
   cd midlus-routine
   ```

2. Compile and package the Android APK using gradle:
   ```bash
   gradle assembleDebug
   ```

3. Run the unit and integration test suites:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 🛰️ Adaptive Device Classes
The interface automatically transitions gracefully between standard mobile devices, large-pane foldable systems, and tablets using **Material Width Classes**, ensuring dynamic negative space and visual integrity are maintained across any temporal node!

*Crafted beautifully with space aesthetics for Nibir Bhuiyan.*
