# Implementation Plan: Sera Instagram Blocker (Open Source Edition)

This document breaks down the high-level `plan.md` into a highly detailed, step-by-step technical implementation plan. Because this will be an open-source GitHub project, we prioritize clean architecture, strict separation of concerns, and defensive programming. This document serves as the "state tracker" across multiple coding sessions to prevent context rot.

---

## Phase 1: Project Initialization & Architecture Setup
**Goal:** Set up a clean, modern Android repository using standard best practices.

1. **Initialize the Android Project:**
   - Use standard modern Android (API 26+).
   - Configure Gradle (`build.gradle.kts`) with dependencies:
     - Jetpack Compose (UI)
     - DataStore Preferences (Persistence)
     - CameraX (Camera)
     - ML Kit Text Recognition (OCR)
     - OkHttp / Retrofit or a lightweight OpenAI-compatible Kotlin client (Networking)
     - Kotlin Coroutines & Flow (Concurrency)
2. **Directory Structure & Packages:**
   - Create foundational packages under `com.example.leetcodegate` (or final package name):
     - `.accessibility` (Service, detectors)
     - `.camera` (CameraX wrappers)
     - `.ocr` (ML Kit extraction logic)
     - `.llm` (Nvidia NIM API clients, models)
     - `.data` (DataStore repositories)
     - `.domain` (CreditManager, usage trackers)
     - `.ui` (Compose screens, ViewModels, navigation)
3. **Manifest & Permissions:**
   - Add `AccessibilityService` configuration in `res/xml/accessibility_service_config.xml`.
   - Add permissions in `AndroidManifest.xml`: `CAMERA`, `INTERNET`, `SYSTEM_ALERT_WINDOW` (if needed for fallbacks), and declare the accessibility service.
4. **Development Config:**
   - Set up `local.properties` injection in `build.gradle.kts` for `NIM_API_KEY` and `NIM_BASE_URL` via `BuildConfig` so the open-source repo remains credential-free.

---

## Phase 2: Local Persistence Layer (DataStore)
**Goal:** Implement the storage mechanisms for tracking credit and completed problems.

1. **AppPreferences:**
   - Create the core DataStore instance.
2. **CreditStore:**
   - Store: `credit_seconds` (Int) and `last_credit_persisted_at` (Long).
   - Implement flow-based observers to read the credit in real-time.
3. **CompletedProblemStore:**
   - Store a `Set<String>` of completed problem IDs (e.g., `["88", "Q1"]`).
   - Implement `isCompleted(id: String): Boolean` and `addCompleted(id: String)`.
4. **SettingsStore:**
   - Store the user's LLM Configuration (`api_key`, `base_url`, `model_name`).
   - Store fallback logic: If DataStore is empty, return `BuildConfig` values.

---

## Phase 3: Core Domain Logic
**Goal:** Build the strict rules engine that governs Instagram access time.

1. **CreditManager:**
   - A singleton or scoped component that acts as the source of truth.
   - Methods: `getCreditFlow()`, `addCredit(seconds: Int)` (caps at 300), `consumeCredit(seconds: Int)`, `isUnlocked()`.
2. **InstagramUsageTracker:**
   - A time-tracking utility that uses `SystemClock.elapsedRealtime()`.
   - Starts tracking when Instagram is detected in the foreground; pauses when minimized or closed.
   - Triggers `CreditManager.consumeCredit()` periodically (e.g., every 1 second via a Coroutine ticker) while Instagram is active.

---

## Phase 4: Accessibility Service Core
**Goal:** Intercept when Instagram is opened and show a blocking mechanism.

1. **InstagramAccessibilityService:**
   - Inherit from `AccessibilityService`.
   - Override `onAccessibilityEvent(AccessibilityEvent)` to listen for `TYPE_WINDOW_STATE_CHANGED`.
   - Identify if the active package is `com.instagram.android`.
2. **ForegroundAppDetector:**
   - Abstract the logic of determining the foreground app. Notifies `InstagramUsageTracker`.
3. **AccessibilityOverlayManager:**
   - When Instagram is active AND credit is 0, construct a full-screen `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`.
   - Inject a ComposeView into this window to render the lock screen.
   - When credit > 0, remove the overlay window.

---

## Phase 5: UI Layer - Lock Screen & Navigation
**Goal:** Build the visual gateway preventing Instagram access.

1. **LockScreen (Compose):**
   - Display a rigid "Instagram Locked" UI.
   - Show a "Take Photo" button that launches the Verification Flow.
2. **Verification Activity/Workflow:**
   - Because CameraX and permissions are tricky inside a WindowManager overlay, the "Take Photo" button should launch a transparent or full-screen `VerificationActivity`.
   - The Activity takes over the UI while the underlying Accessibility Service pauses its aggressive overlay (or replaces it with the Activity).

---

## Phase 6: Camera Integration
**Goal:** Allow users to capture their screen natively.

1. **CameraScreen (Compose):**
   - Embed a `Preview` use case using CameraX.
   - Implement an overlay framing the area where the user should position the LeetCode problem.
2. **PhotoCaptureManager:**
   - Handle the `ImageCapture` use case.
   - Capture a high-res image to temporary cache storage.
   - Delete the image immediately after OCR processing.

---

## Phase 7: OCR & Problem Extraction
**Goal:** Extract the exact problem number locally to prevent duplicate network calls.

1. **OcrEngine:**
   - Feed the captured image into ML Kit's `TextRecognition.getClient()`.
2. **ProblemNumberExtractor:**
   - Implement **Spatial Ranking**: Extract bounding boxes and prioritize large, top-left text.
   - Implement **Regex Parsers**:
     - `^\s*(Q?\d{1,5})\.\s+[A-Z]`
     - `^\s*(Q?\d{1,5})\s+[-|]?\s*[A-Z]`
   - Implement **Contextual Anchors**: Search for "Description" and "Editorial" text blocks to locate the title.
3. **Local Validation:**
   - If a problem number is extracted, query `CompletedProblemStore`. If it exists, reject locally and prompt the user to pick a new problem.

---

## Phase 8: LLM Validation (Nvidia NIM / OpenAI)
**Goal:** Validate the user's understanding of the LeetCode problem.

1. **ExplanationScreen (Compose):**
   - Present the extracted Problem Number and Title.
   - Provide a text field requiring >30 characters of explanation.
2. **LlmValidator:**
   - Use OkHttp or an OpenAI-compatible Retrofit interface.
   - Read the user's `SettingsStore` for the API Key, Base URL, and Model.
   - Construct the prompt explicitly defining the JSON schema response: `{"pass": true/false, "detected_problem_number": "...", "reason": "..."}`.
3. **Transaction Safety:**
   - Only on a successful LLM `pass == true`:
     1. Save the problem to `CompletedProblemStore`.
     2. Add 300 seconds to `CreditManager`.
     3. Close the Verification Activity.

---

## Phase 9: Settings & App Management UI
**Goal:** Provide user controls and fallback monitoring.

1. **SettingsScreen (Compose):**
   - Show Accessibility Service status (Enabled/Disabled) with a button routing to Android System Settings.
   - Show LLM Configuration text fields (API Key, Base URL, Model) updating `SettingsStore`.
   - "Test Protection" and "Clear Credit" buttons for debugging.
2. **HomeScreen (Compose):**
   - Main dashboard for the app.
   - Shows remaining credit with a live ticker.
   - Shows total problems completed.

---

## Phase 10: Open Source Polish & Hardening
**Goal:** Ensure the app is stable, bug-free, and attractive on GitHub.

1. **Lifecycle Resilience:**
   - Ensure the app handles being force-closed (CreditStore must recover accurate time).
   - Ensure the Accessibility Service restarts cleanly on device boot.
2. **Error Handling:**
   - Comprehensive error UI for Network Failures (LLM timeout), Camera Failures, and OCR parsing errors.
3. **Documentation:**
   - Write a stellar `README.md` containing:
     - The "Why" behind the app.
     - Architecture overview.
     - How to build locally (configuring `local.properties`).
     - How to get a free Nvidia NIM API key.
4. **License:**
   - Add MIT License file.

---

## How to use this plan across sessions
When beginning a new session, the AI agent should:
1. Read this `implementation_plan.md`.
2. Check the Git commit history and the codebase to determine the current phase.
3. Pick up the next logical uncompleted phase.
4. Execute the phase cleanly, testing thoroughly before proceeding to the next.
