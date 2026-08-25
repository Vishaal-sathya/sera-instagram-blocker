# LeetCode Instagram Gate --- Detailed Build Plan

## 1. Project Goal

Build a personal Android app that prevents Instagram from being used
unless the user has successfully demonstrated that they just completed a
new LeetCode problem.

The app is intentionally local-first:

-   No user accounts.
-   No backend.
-   No cloud database.
-   No LeetCode API.
-   No cloud OCR.
-   No problem database.
-   No cloud storage.
-   The only network dependency is the LLM validation request.
-   The local persistent state only records which LeetCode problem
    numbers have already been successfully submitted and the current
    Instagram usage credit.
-   A successful verification grants **5 minutes of Instagram usage
    credit**.
-   The 5 minutes are a **consumable balance**, not a five-minute
    countdown that starts at submission.
-   The balance is consumed only while Instagram is actually in the
    foreground.
-   When the balance reaches zero, Instagram is blocked again.

The first implementation should prioritize reliability and simplicity
over visual polish.

------------------------------------------------------------------------

# 2. Core User Experience

## Initial state

After installing the app:

1.  User opens the app.
2.  App explains that it needs Accessibility Service permission.
3.  User enables the service.
4.  User verifies that Instagram protection is active.
5.  Instagram starts with `0` available seconds.
6.  Opening Instagram immediately shows the lock screen.

The user cannot unlock Instagram through a normal "skip" button.

## Unlock flow

``` text
Open Instagram
      |
      v
Is Instagram credit > 0?
      |
   +--+--+
   |     |
  YES    NO
   |     |
   v     v
Allow   Lock screen
          |
          v
       Take photo
          |
          v
       OCR photo
          |
          v
   Extract LeetCode #
          |
          v
 Already submitted?
      |          |
     YES         NO
      |           |
    Reject        v
              Ask explanation
                   |
                   v
              LLM validation
                |       |
              FAIL     PASS
                |       |
              Retry    Save problem #
                         |
                         v
                    Set credit to 5 min
                         |
                         v
                    Unlock Instagram
```

------------------------------------------------------------------------

# 3. Important Interpretation of the 5-Minute Rule

The app must NOT implement:

``` text
Verification at 10:00
Instagram available until 10:05
```

Instead it must implement:

``` text
credit = 300 seconds

Instagram active for 90 sec
credit = 210 sec

Instagram closed
credit remains = 210 sec

Instagram reopened later
credit = 210 sec

Instagram active for another 210 sec
credit = 0

Instagram becomes blocked
```

Closing Instagram pauses the balance.

Opening another application pauses the balance.

Only actual Instagram foreground time consumes credit.

A successful verification should grant:

``` text
credit = min(currentCredit + 300, 300)
```

The cap should remain **300 seconds**. This prevents banking unlimited
Instagram time by completing many problems.

If the user has 2 minutes remaining and successfully completes another
problem, the balance becomes 5 minutes rather than 7 minutes.

This is the recommended interpretation of "each photo unlocks it for 5
minutes only."

------------------------------------------------------------------------

# 4. Technology Stack

Use a **native Android application**.

## Recommended stack

  Area                      Technology
  ------------------------- ---------------------------------------------------------------
  Language                  Kotlin
  UI                        Jetpack Compose
  Architecture              Simple MVVM / repository pattern
  Instagram detection       Android AccessibilityService
  Blocking UI               Accessibility overlay
  Camera                    CameraX
  OCR                       Google ML Kit Text Recognition
  Local persistence         DataStore Preferences
  Networking                OkHttp or an OpenAI compatible client library (e.g. openai-kotlin)
  LLM                       Nvidia NIM model initially, with model name configurable
  Minimum Android version   API 26+ is a reasonable initial target
  Backend                   None
  Authentication            None
  Cloud database            None

Do not introduce Room unless the implementation later proves that
DataStore is insufficient.

The persistent state is tiny, so a key/value store is appropriate.

ML Kit's Android Text Recognition API supports extracting text from
images and can use a bundled model, which is useful for keeping OCR
local.

Official reference:
https://developers.google.com/ml-kit/vision/text-recognition/v2/android

Android's AccessibilityService API supports accessibility overlays,
which is the mechanism the app should use to place the lock UI over the
target application.

Official reference:
https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

------------------------------------------------------------------------

# 5. Why Native Android Instead of React Native

Do not start with React Native for this project.

The difficult part is not the UI. The difficult part is Android system
integration:

-   AccessibilityService lifecycle.
-   Detecting the active package.
-   Displaying a blocking overlay.
-   Maintaining the service.
-   Handling Android lifecycle edge cases.
-   Returning to Instagram after successful verification.
-   Handling Android settings and permissions.

Jetpack Compose can still make the UI straightforward.

If the UI later needs to become cross-platform, React Native can be
reconsidered, but the core Android service would still need native code.

------------------------------------------------------------------------

# 6. High-Level Architecture

``` text
                    Android OS
                        |
                        v
              AccessibilityService
                        |
                        | detects package
                        v
               Instagram detected
                        |
                        v
                 CreditManager
                    /       \
                   /         \
              credit > 0    credit == 0
                  |              |
                  v              v
              allow          LockOverlay
                                 |
                                 v
                           Verification UI
                                 |
                    +------------+-------------+
                    |                          |
                    v                          v
                 Camera                     DataStore
                    |                          |
                    v                          |
                 OCR                          |
                    |                          |
                    v                          |
            Problem number                     |
                    |                          |
                    v                          |
             CompletedProblemStore <-----------+
                    |
                    v
             Already submitted?
               /           \
             YES            NO
              |              |
            reject           v
                       Explanation
                           |
                           v
                    LlmValidator
                           |
                     +-----+-----+
                     |           |
                    FAIL        PASS
                     |           |
                   retry         v
                           Record problem
                               |
                               v
                          Set credit to 300
                               |
                               v
                         Hide lock overlay
```

------------------------------------------------------------------------

# 7. Suggested Project Structure

Use a structure similar to:

``` text
app/
└── src/main/
    ├── AndroidManifest.xml
    │
    ├── java/com/example/leetcodegate/
    │   │
    │   ├── MainActivity.kt
    │   ├── App.kt
    │   │
    │   ├── accessibility/
    │   │   ├── InstagramAccessibilityService.kt
    │   │   ├── AccessibilityOverlayManager.kt
    │   │   └── ForegroundAppDetector.kt
    │   │
    │   ├── camera/
    │   │   ├── CameraScreen.kt
    │   │   ├── CameraController.kt
    │   │   └── PhotoCaptureManager.kt
    │   │
    │   ├── ocr/
    │   │   ├── OcrEngine.kt
    │   │   └── ProblemNumberExtractor.kt
    │   │
    │   ├── verification/
    │   │   ├── VerificationRepository.kt
    │   │   ├── LlmValidator.kt
    │   │   ├── VerificationResult.kt
    │   │   └── PromptBuilder.kt
    │   │
    │   ├── storage/
    │   │   ├── AppPreferences.kt
    │   │   ├── CompletedProblemStore.kt
    │   │   └── CreditStore.kt
    │   │
    │   ├── credit/
    │   │   ├── CreditManager.kt
    │   │   └── InstagramUsageTracker.kt
    │   │
    │   ├── ui/
    │   │   ├── HomeScreen.kt
    │   │   ├── LockScreen.kt
    │   │   ├── ExplanationScreen.kt
    │   │   ├── VerificationResultScreen.kt
    │   │   ├── HistoryScreen.kt
    │   │   └── SettingsScreen.kt
    │   │
    │   └── model/
    │       ├── VerificationState.kt
    │       └── AppState.kt
    │
    └── res/
        ├── xml/
        │   └── accessibility_service_config.xml
        └── values/
```

The exact package names can be changed by the coding agent.

------------------------------------------------------------------------

# 8. Persistent State

Keep persistent storage intentionally tiny.

## Required state

### Completed problems

Store only:

``` text
completed_problem_numbers = [1, 15, 49, 88, 121]
```

No problem descriptions.

No LeetCode solutions.

No screenshots.

No explanation history is required.

No cloud synchronization.

## Instagram credit

Store:

``` text
credit_seconds = 137
```

Also store enough information to correctly reconcile the balance when
the service is restarted.

Example:

``` text
credit_seconds = 137
last_credit_update_timestamp = ...
instagram_was_active = true/false
```

The implementation should use a monotonic clock such as
`SystemClock.elapsedRealtime()` while the service/process is alive to
measure foreground duration.

For persistence across process death/reboot, use wall-clock timestamps
only as a recovery mechanism and never allow clock changes to create
unlimited credit.

The safest simple behavior for the first version is:

-   Persist the remaining credit frequently.
-   When Instagram is active, decrement credit using elapsed foreground
    duration.
-   On reboot/service restart, recover the last persisted credit.
-   If the exact amount cannot be reconstructed safely, round toward the
    safer state rather than granting additional time.

Never allow a clock manipulation to increase Instagram credit.

------------------------------------------------------------------------

# 9. DataStore Design

A simple Preferences DataStore is sufficient.

Suggested keys:

``` text
completed_problem_numbers
credit_seconds
last_credit_persisted_at
service_enabled
```

The completed problem list can be stored as a `Set<String>`.

Example:

``` text
completedProblemNumbers = {"1", "15", "49", "88"}
```

Do not use a database schema with multiple tables unless future
requirements demand it.

------------------------------------------------------------------------

# 10. AccessibilityService

This is the central Android component.

## Responsibilities

The service should:

1.  Receive accessibility events.
2.  Detect when the active package is Instagram.
3.  Check current credit.
4.  Allow Instagram when credit \> 0.
5.  Show the lock overlay when credit == 0.
6.  Continuously track whether Instagram remains active.
7.  Deduct credit only while Instagram is foreground.
8.  Remove the lock overlay when Instagram is no longer active.
9.  Launch the verification Activity when the user chooses to unlock.
10. Never interfere with unrelated applications.

Instagram's package identifier is normally:

``` text
com.instagram.android
```

Do not hardcode this everywhere. Define:

``` kotlin
const val INSTAGRAM_PACKAGE = "com.instagram.android"
```

in one place.

If possible, expose the package as configuration so it can be changed
later.

------------------------------------------------------------------------

# 11. Accessibility Events

The service should listen for window-state changes.

The main event of interest is:

``` text
TYPE_WINDOW_STATE_CHANGED
```

and potentially:

``` text
TYPE_WINDOWS_CHANGED
```

depending on the implementation.

On every relevant event:

``` text
currentPackage = event.packageName

if currentPackage == INSTAGRAM_PACKAGE:
    handleInstagramForeground()
else:
    handleOtherAppForeground()
```

Do not perform expensive OCR or network requests from the
AccessibilityService.

The service should remain lightweight.

------------------------------------------------------------------------

# 12. Blocking Overlay

When Instagram opens and credit is zero:

``` text
Instagram UI
      |
      v
Full-screen accessibility overlay
      |
      v
Your LockScreen UI
```

The overlay must consume touch input so the user cannot interact with
Instagram underneath it.

The overlay should contain:

``` text
Instagram Locked

Complete a new LeetCode problem
to unlock 5 minutes.

[ Take Photo ]
```

Optional:

``` text
Completed problems: 12
```

Do not display the Instagram UI underneath in a way that makes it
usable.

Android supports accessibility overlay windows specifically for service
UI layered over other windows.

------------------------------------------------------------------------

# 13. Do Not Use a Normal Activity as the Only Lock Mechanism

A normal Activity is not sufficient as the sole enforcement mechanism.

The app must be able to react when the user opens Instagram from:

-   launcher
-   recent apps
-   notification
-   another application
-   system navigation
-   deep links

The AccessibilityService is the enforcement layer.

The Activity/Compose UI is the verification workflow.

------------------------------------------------------------------------

# 14. Verification UI

When the user taps:

``` text
Take Photo
```

the service should launch/open the app's verification Activity.

The verification Activity should be full-screen.

State:

``` text
VerificationState.CapturingPhoto
```

Then:

``` text
VerificationState.ProcessingPhoto
```

Then:

``` text
VerificationState.ProblemDetected(problemNumber)
```

Then:

``` text
VerificationState.AskingExplanation
```

Then:

``` text
VerificationState.Validating
```

Then either:

``` text
VerificationState.Success
```

or:

``` text
VerificationState.Failure
```

------------------------------------------------------------------------

# 15. Camera Requirements

The user specifically wants a **photo taken from the phone**, not an
uploaded screenshot.

Therefore:

-   Do not provide a gallery picker in the first version.
-   Do not allow selecting an existing image.
-   Open CameraX directly.
-   Capture the image inside the app.
-   Immediately process the captured image.
-   Delete the temporary image after processing unless debugging is
    enabled.

The app cannot mathematically prove that the photographed object is a
physical screen rather than another display showing a screenshot. Do not
attempt elaborate anti-cheat computer vision in v1.

The requirement is simply:

> The user must capture a new photo through the app camera.

------------------------------------------------------------------------

# 16. Camera UX

Use a simple camera screen:

``` text
--------------------------------
|                              |
|        Camera Preview        |
|                              |
|     ┌──────────────────┐     |
|     │                  │     |
|     │  LeetCode screen │     |
|     │                  │     |
|     └──────────────────┘     |
|                              |
|          [ CAPTURE ]         |
--------------------------------
```

After capture:

``` text
Analyzing photo...
```

No need for manual cropping initially.

------------------------------------------------------------------------

# 17. OCR

Use ML Kit Text Recognition.

The OCR pipeline:

``` text
CameraX image
      |
      v
InputImage
      |
      v
ML Kit TextRecognizer
      |
      v
RecognizedText
      |
      v
ProblemNumberExtractor
```

The extracted OCR text might look like:

``` text
88. Merge Sorted Array

You are given two integer arrays nums1 and nums2...
```

The extractor should identify:

``` text
88
```

------------------------------------------------------------------------

# 18. Problem Number Extraction

Extracting the exact problem number from raw OCR text can be tricky. LeetCode problems could be just numbers (e.g., "88") or start with a 'Q' for quests (e.g., "Q1").

We must build a highly reliable extraction strategy by combining **Spatial Ranking**, **Contextual Anchors**, and **Smart Regex**.

### Spatial Ranking
ML Kit Text Recognition returns TextBlock objects with bounding boxes (X/Y coordinates on the screen). The problem title ("88. Merge Sorted Array") has two unique physical characteristics:
- It is physically located near the top-left quadrant of the screen.
- It is usually rendered in the **largest font size** on the page.

Rank OCR blocks based on their size and position.
Example:
`kotlin
val bestCandidate = textBlocks
    .filter { it.boundingBox.top < screenHeight / 2 } // Upper half
    .maxByOrNull { it.boundingBox.height() } // Largest text
`

### Smart Regex Patterns
Once the likely title blocks are isolated, run them through prioritized Regex patterns that support both standard and 'Q'-prefixed problem numbers.

**Pattern 1 (High Confidence):**
`
egex
^\s*(Q?\d{1,5})\.\s+[A-Z]
`
- Matches: "88. Merge Sorted Array", "Q1. Two Sum"

**Pattern 2 (Medium Confidence - OCR missed the dot):**
`
egex
^\s*(Q?\d{1,5})\s+[-|]?\s*[A-Z]
`
- Matches: "88 Merge Sorted Array", "Q1 - Two Sum"

### Contextual Anchors
If the regex on the largest text block fails, look for "anchors". Often, right above the title are the tabs: Description | Editorial | Solutions | Submissions. If we find a line containing "Description" and "Editorial", the problem title is almost always the block immediately below it.

### LLM Safety Net
If multiple candidates exist or local extraction is uncertain, rank them and pick the highest confidence one to check against the local completed database. Since the full OCR text is sent to the LLM anyway, we can instruct the LLM to double-check the problem number. If the LLM's detected_problem_number contradicts the local heuristic, either trust the LLM or prompt the user for a clearer photo.

------------------------------------------------------------------------

# 19. OCR Validation

The app should not immediately accept a problem number merely because it
found a number.

Require evidence that the photo is plausibly a LeetCode problem.

Possible checks:

``` text
contains "leetcode"
OR
contains recognizable title/header structure
OR
contains phrases such as:
    "Example"
    "Input"
    "Output"
    "Constraints"
```

Because the photo is a physical photo rather than a screenshot, OCR may
be imperfect.

The goal is not perfect document authentication.

The goal is to prevent accidental/obvious invalid submissions.

------------------------------------------------------------------------

# 20. Duplicate Problem Check

After extracting the problem number:

``` kotlin
if (completedProblemStore.contains(problemNumber)) {
    showAlreadyCompleted()
    return
}
```

Display:

``` text
Problem #88

Already completed.

Choose a different LeetCode problem.
```

Do not call the LLM for an already-completed problem.

This saves API requests.

------------------------------------------------------------------------

# 21. Important Duplicate Rule

Only mark a problem as completed **after the LLM says PASS**.

Never mark it completed after:

-   successful OCR
-   photo capture
-   explanation submission
-   LLM request started
-   LLM request failed
-   LLM returned malformed output

The transaction should effectively be:

``` text
OCR problem #88
    ↓
Check database
    ↓
Not completed
    ↓
LLM PASS
    ↓
SAVE #88
    ↓
Set credit to 300 seconds
```

If LLM returns FAIL:

``` text
DO NOT SAVE #88
DO NOT ADD CREDIT
```

------------------------------------------------------------------------

# 22. Explanation Screen

After identifying a new problem:

``` text
Problem #88 detected

Merge Sorted Array

Explain how you solved it.

Do not look up the solution.
Explain the approach in your own words.

[ Text field ]

[ SUBMIT ]
```

The app should not show the official solution.

It can show only:

-   problem number
-   title extracted from the photo
-   user's explanation input

Do not automatically solve the problem for the user.

------------------------------------------------------------------------

# 23. Minimum Explanation Requirements

Before sending to the LLM:

``` text
trim whitespace

if length < 30:
    reject locally
```

Optionally require:

``` text
wordCount >= 8
```

This prevents requests such as:

``` text
"two pointers"
```

from reaching the LLM.

However, don't make the local rule too strict because concise
explanations can still be valid.

Recommended first version:

``` text
minimum 30 characters
```

------------------------------------------------------------------------

# 24. What Should Be Sent to the LLM

Do NOT send the photo if OCR has already extracted enough information.

The request should contain:

``` text
Problem number
Problem title
OCR text from the photo
User explanation
```

Example:

``` text
Problem number: 88
Problem title: Merge Sorted Array

OCR text:
88. Merge Sorted Array
You are given two integer arrays nums1 and nums2...

User explanation:
I would use three pointers. One points to the end
of nums1's actual values, one to the end of nums2,
and one to the final position in nums1. I compare
the values from the back so I don't overwrite values
that I still need.
```

The OCR text provides the model with the actual problem context.

This avoids needing a local LeetCode problem database.

------------------------------------------------------------------------

# 25. LLM Prompt

The prompt should be strict and deterministic.

Use a structure like:

``` text
You are a LeetCode understanding validator.

Your job is NOT to solve the problem for the user.

Determine whether the user's explanation demonstrates
a genuine understanding of the problem and their claimed
solution approach.

You have:
- Problem number
- Problem title
- OCR text from the photographed problem
- User's explanation

PASS if:
- The explanation describes a plausible solution.
- The core algorithmic idea is correct.
- The explanation is specific enough to indicate understanding.
- Minor wording mistakes do not matter.
- The user does not need perfect code.

FAIL if:
- The explanation is vague or meaningless.
- The approach is fundamentally incorrect.
- The user merely repeats the title.
- The response is unrelated.
- The explanation clearly indicates they do not understand
  the solution.

Do not require exact terminology.

Return ONLY valid JSON:

{
  "pass": true,
  "confidence": 0.0,
  "reason": "short explanation"
}
```

The app should parse JSON.

Do not rely on natural-language output such as:

``` text
"Yes, I think they understand..."
```

------------------------------------------------------------------------

# 26. LLM Decision Policy

The app should be conservative.

Suggested logic:

``` text
if API request fails:
    no credit
    allow retry

if JSON cannot be parsed:
    no credit
    allow retry

if pass == false:
    no credit

if pass == true:
    accept
```

Do not require a particular confidence threshold initially.

If testing reveals false positives, introduce:

``` text
confidence >= 0.75
```

later.

Do not overengineer this before collecting real examples.

------------------------------------------------------------------------

# 27. Nvidia NIM API (OpenAI Compatible)

Use Nvidia NIM which provides access to multiple models. Because most NIM models are OpenAI compatible, the app should use an OpenAI compatible API client, changing the ase_url to point to Nvidia NIM.

For this application, the expected request volume is tiny:

` text
~10 validations/day
~300/month
`

The first implementation should target a fast instruct model available on Nvidia NIM.

Do not hardcode a model name, base URL, or API key permanently. They should be configurable.

Create:

` kotlin
object LlmConfig {
    var BASE_URL = "https://integrate.api.nvidia.com/v1"
    var MODEL = "meta/llama-3.1-8b-instruct"
}
`

The app will read the API key and Base URL from user settings, but will fall back to a build config during development.

------------------------------------------------------------------------

# 28. API Key & Base URL Strategy

For a personal APK, the simplest implementation is:

` text
Android app
    |
    v
Nvidia NIM (OpenAI Compatible) API
`

Because the user wants flexibility, the app must allow the user to set the API Key and Base URL **post-installation** through the Settings screen.

However, for development purposes, the app should also read from a local build configuration (local.properties) to pre-fill or fall back to these values.

Do NOT build a backend just to solve this problem in v1. The direct API call from the app is acceptable for a personal application.

------------------------------------------------------------------------

# 29. API Key Configuration (Development)

Never commit the real API key to Git.

For development, use:

` text
local.properties
`

Example concept:

` text
NIM_API_KEY=YOUR_KEY_HERE
NIM_BASE_URL=https://integrate.api.nvidia.com/v1
`

The agent should configure Gradle so the key is injected into the app build as a BuildConfig field. The app will use this BuildConfig value as the default if the user hasn't configured a key in the app settings.

Add local.properties to .gitignore.

Also create local.properties.example containing:

` text
NIM_API_KEY=YOUR_KEY_HERE
NIM_BASE_URL=https://integrate.api.nvidia.com/v1
`

------------------------------------------------------------------------

# 30. LLM Request Failure Handling

Possible failures:

``` text
No internet
API timeout
Rate limit
Invalid API key
Server error
Malformed response
Safety refusal
Model unavailable
```

All must result in:

``` text
No Instagram credit granted.
Problem remains uncompleted.
User can retry.
```

The user should see something like:

``` text
Verification unavailable.

Your problem was NOT marked as completed.

Check your internet connection and retry.
```

Do not permanently consume the problem because of an API failure.

------------------------------------------------------------------------

# 31. Success Transaction

A successful verification must perform these operations in order:

``` text
1. Verify LLM PASS.
2. Add problem number to completed set.
3. Set credit to 300 seconds.
4. Persist both changes.
5. Close verification UI.
6. Return user to Instagram.
```

If persistence fails, do not grant access.

The app should prefer a false lock over accidentally granting unlimited
access.

------------------------------------------------------------------------

# 32. CreditManager

Create a single source of truth:

``` kotlin
class CreditManager
```

Responsibilities:

``` text
getCreditSeconds()
setCredit()
grantUnlock()
consumeCredit()
isUnlocked()
```

Do not let the AccessibilityService directly manipulate DataStore
values.

Instead:

``` text
AccessibilityService
        |
        v
CreditManager
        |
        v
DataStore
```

This keeps business logic testable.

------------------------------------------------------------------------

# 33. InstagramUsageTracker

Create:

``` kotlin
class InstagramUsageTracker
```

Responsibilities:

``` text
startTracking()
pauseTracking()
update()
getRemainingCredit()
```

Conceptually:

``` kotlin
if (instagramBecameForeground) {
    trackingStart = elapsedRealtime()
}

if (instagramLeftForeground) {
    elapsed = elapsedRealtime() - trackingStart
    credit -= elapsed
}
```

Also periodically persist while Instagram remains open.

For example:

``` text
every 1 second:
    update credit
```

A 1-second update interval is acceptable for a personal app.

If battery usage becomes noticeable, increase the persistence interval
while maintaining accurate in-memory accounting.

------------------------------------------------------------------------

# 34. What Happens When Instagram Is Minimized?

Example:

``` text
Credit = 180 sec

Instagram open
    ↓
Use for 40 sec
    ↓
Credit = 140 sec

Home button
    ↓
Tracking stops

Credit remains 140 sec
```

When Instagram returns:

``` text
Resume tracking
```

------------------------------------------------------------------------

# 35. What Happens If Instagram Is Force-Stopped?

If Instagram is force-stopped:

``` text
No foreground usage
```

Credit remains whatever was persisted.

When Instagram is opened again:

``` text
Resume normal enforcement
```

------------------------------------------------------------------------

# 36. What Happens If the Phone Reboots?

On reboot:

-   AccessibilityService may not immediately be running.
-   Android may require the service to be restored.
-   Do not assume the service is instantly active.

The app should provide a settings screen showing:

``` text
Protection:
✓ Accessibility service enabled
```

If disabled:

``` text
Protection:
✗ Accessibility service disabled

[ Enable Protection ]
```

The app cannot prevent the user from disabling the service through
Android settings.

------------------------------------------------------------------------

# 37. Bypass Resistance

This is a self-control app, not a security product.

Do not promise impossible protection.

The user can theoretically:

-   disable the AccessibilityService
-   uninstall the app
-   use another Instagram client
-   use a browser
-   modify the phone
-   disable permissions

The app should focus on preventing **normal impulsive Instagram
opening**, not defeating a determined owner.

For v1:

``` text
Target:
Normal Instagram app

Do not attempt:
Browser Instagram
Instagram Lite
VPN-level blocking
Device-owner management
Root detection
Tamper protection
```

These can be future features.

------------------------------------------------------------------------

# 38. Settings Screen

The main settings screen should include:

` text
Protection
--------------------------------

Accessibility Service
[ Enabled / Disabled ]

Instagram package
com.instagram.android

Unlock duration
5 minutes

Maximum credit
5 minutes

Require new problem
Enabled

LLM validation
Enabled

LLM Configuration
--------------------------------
API Key: [ ******************** ]
Base URL: [ https://integrate.api.nvidia.com/v1 ]
Model: [ meta/llama-3.1-8b-instruct ]

[ Test Protection ]

[ Reset Completed Problems ]

[ Clear Instagram Credit ]
`

Because the user explicitly wants one submission to equal five minutes, keep those values fixed in v1 even if the UI displays them. The LLM Configuration (API Key, Base URL, Model) should be editable here.

------------------------------------------------------------------------

# 39. Home Screen

Simple dashboard:

``` text
LeetCode Gate

Instagram
LOCKED

Available credit
00:00

Problems completed
12

Protection
ACTIVE

[ Test Instagram Lock ]

[ History ]

[ Settings ]
```

When unlocked:

``` text
LeetCode Gate

Instagram
UNLOCKED

Remaining
03:42

Problems completed
12
```

The timer should update live.

------------------------------------------------------------------------

# 40. History Screen

Because the user only wants the local database to track whether a
problem has been submitted, history should be minimal.

Show:

``` text
Completed problems

#1
#15
#49
#88
#121
```

Do not store explanation text unless explicitly added as a future
feature.

Do not store photos.

Do not store LLM responses.

This keeps local data minimal.

------------------------------------------------------------------------

# 41. Privacy Model

The app should follow:

``` text
Photo:
captured temporarily
      |
      v
OCR locally
      |
      v
temporary image deleted

OCR text:
used for current validation
      |
      v
sent to the LLM only when required

Explanation:
sent to the LLM
      |
      v
discarded after validation

Completed problem number:
stored locally
```

Do not upload images to your own server.

Do not create analytics.

Do not create accounts.

Do not add Firebase.

Do not add advertisements.

Do not add crash-reporting SDKs initially.

------------------------------------------------------------------------

# 42. Should the Image Be Sent to the LLM?

Default answer: **No.**

The photo is only required to identify the problem.

Use:

``` text
Photo
 -> ML Kit OCR
 -> problem number/title/OCR text
 -> LLM receives text + explanation
```

This is cheaper, more private, and simpler.

However, structure the code so a multimodal fallback can be added later
if OCR quality proves insufficient.

For example:

``` kotlin
interface ProblemValidatorInput
```

could later support:

``` text
TextOnlyInput
ImageAndTextInput
```

Do not implement the multimodal path in v1 unless OCR testing
demonstrates a real need.

------------------------------------------------------------------------

# 43. OCR Failure Recovery

If OCR cannot identify the problem:

``` text
Could not identify the LeetCode problem.

Make sure:
- the problem title is visible
- the problem number is visible
- the image is in focus

[ Retake Photo ]
```

Do not provide a manual problem-number field in v1.

Why?

Because a manual field would let the user type:

``` text
88
```

without photographing anything.

If later necessary, a manual correction step can be added only after OCR
has already found a candidate.

------------------------------------------------------------------------

# 44. Handling Different LeetCode Page Layouts

The OCR extractor should not depend on the exact screenshot/photo in the
example.

Support:

``` text
88. Merge Sorted Array
88 Merge Sorted Array
#88 Merge Sorted Array
88. Merge Sorted Array - LeetCode
Problem 88
```

The implementation should rely on text patterns and title-like structure
rather than exact coordinates.

Do not use image-coordinate hardcoding.

------------------------------------------------------------------------

# 45. Preventing Duplicate Submissions

The completed set should be checked before the LLM call.

Example:

``` text
Photo -> #88
       |
       v
CompletedProblems.contains(88)
       |
      YES
       |
       v
Reject immediately
```

This is important because duplicate attempts should not consume free LLM
quota.

------------------------------------------------------------------------

# 46. What If the User Submits a Problem and LLM Says FAIL?

Show:

``` text
Not verified.

Your explanation did not demonstrate
enough understanding of the problem.

Try explaining:
- the main algorithm
- why it works
- important edge cases

[ TRY AGAIN ]
[ RETAKE PHOTO ]
```

Do not reveal a complete solution.

The feedback should be intentionally limited.

Otherwise the app can become a solution-generation tool instead of a
learning gate.

------------------------------------------------------------------------

# 47. LLM Feedback

The model can return:

``` json
{
  "pass": false,
  "confidence": 0.32,
  "reason": "The explanation describes sorting the array, which does not address the required in-place merge approach."
}
```

The UI should display only the short `reason`.

Do not display hidden model reasoning.

Do not ask the model for chain-of-thought.

------------------------------------------------------------------------

# 48. Anti-Cheat Philosophy

Do not attempt to build a sophisticated anti-cheat system.

The objective is:

> Make opening Instagram require enough deliberate effort that the user
> naturally chooses to keep working on LeetCode.

The app should not become a security project.

Useful friction:

-   Camera required.
-   New photo required.
-   Problem must be recognized.
-   Problem cannot already be completed.
-   Explanation required.
-   Explanation must pass LLM validation.
-   5-minute maximum credit.

That is enough.

------------------------------------------------------------------------

# 49. Initial MVP

The first working version should contain ONLY:

### Required

-   [ ] Android project
-   [ ] Compose UI
-   [ ] AccessibilityService
-   [ ] Detect Instagram
-   [ ] Block Instagram when credit is zero
-   [ ] CameraX capture
-   [ ] ML Kit OCR
-   [ ] Extract problem number
-   [ ] Store completed problem numbers
-   [ ] Explanation input
-   [ ] LLM validation
-   [ ] Grant exactly 5 minutes on success
-   [ ] Consume credit only during Instagram foreground usage
-   [ ] Persist credit
-   [ ] Persist completed problems
-   [ ] Basic settings
-   [ ] Basic error handling

### Explicitly NOT required

-   [ ] Accounts
-   [ ] Backend
-   [ ] Firebase
-   [ ] Room
-   [ ] LeetCode API
-   [ ] Full problem database
-   [ ] Cloud OCR
-   [ ] Photo storage
-   [ ] Analytics
-   [ ] Push notifications
-   [ ] Social features
-   [ ] Fancy animations
-   [ ] Multiple blocked apps
-   [ ] Browser blocking
-   [ ] Anti-root protection

------------------------------------------------------------------------

# 50. Development Phases

The coding agent should implement the project in phases.

Do not attempt to build everything in one pass.

------------------------------------------------------------------------

## Phase 0 --- Environment Verification

Before writing application code:

-   [ ] Verify Android SDK is installed.
-   [ ] Verify Gradle/Android build works.
-   [ ] Verify a blank Compose application builds.
-   [ ] Verify a physical Android device is available.
-   [ ] Verify USB debugging can be enabled.
-   [ ] Install the blank APK.
-   [ ] Confirm Android version.
-   [ ] Confirm Instagram is installed.
-   [ ] Record the device Android version for testing.

If Android Studio is unavailable, the agent should determine whether the
local Android SDK and Gradle tooling are already available.

Do not assume Android Studio is mandatory for building an APK if
command-line Android SDK/Gradle tooling is available.

------------------------------------------------------------------------

# 51. Phase 1 --- Basic Application

Build:

``` text
MainActivity
HomeScreen
SettingsScreen
```

Add:

``` text
AppState
```

with:

``` text
creditSeconds
completedProblems
serviceEnabled
```

Initially use fake values.

Acceptance test:

``` text
App launches.
Home screen renders.
Settings screen renders.
No crash after process restart.
```

------------------------------------------------------------------------

# 52. Phase 2 --- DataStore

Implement:

``` text
AppPreferences
CompletedProblemStore
CreditStore
```

Tests:

``` text
save #88
restart app
read #88
```

Expected:

``` text
#88 still exists
```

Test:

``` text
credit = 300
restart
```

Expected:

``` text
credit remains approximately 300
```

Do not grant additional credit after restart.

------------------------------------------------------------------------

# 53. Phase 3 --- AccessibilityService

Implement the service.

Acceptance test:

``` text
Open another app
```

Expected:

``` text
No overlay
```

Then:

``` text
Open Instagram with credit = 0
```

Expected:

``` text
Lock overlay appears
```

Then:

``` text
Open another app
```

Expected:

``` text
Lock overlay disappears
```

Then:

``` text
Open Instagram with credit = 300
```

Expected:

``` text
No blocking overlay
```

------------------------------------------------------------------------

# 54. Phase 4 --- Credit Consumption

Start with a fake unlock button.

For testing:

``` text
[ ADD 5 MINUTES ]
```

Then open Instagram.

Expected:

``` text
05:00
04:59
04:58
...
```

Leave Instagram.

Expected:

``` text
timer stops
```

Return to Instagram.

Expected:

``` text
timer resumes
```

When:

``` text
00:00
```

Expected:

``` text
lock overlay immediately appears
```

This phase should be completed and stable before adding OCR.

------------------------------------------------------------------------

# 55. Phase 5 --- CameraX

Implement camera capture.

Acceptance:

-   [ ] Camera permission requested.
-   [ ] Camera preview opens.
-   [ ] Capture button works.
-   [ ] Image is captured.
-   [ ] Image can be passed to OCR.
-   [ ] No gallery picker exists.
-   [ ] Temporary image is deleted after processing.

Test using the example image photographed from another screen.

------------------------------------------------------------------------

# 56. Phase 6 --- OCR

Integrate ML Kit.

Create:

``` kotlin
interface OcrEngine {
    suspend fun recognizeText(image: InputImage): String
}
```

Then:

``` kotlin
class MlKitOcrEngine : OcrEngine
```

This abstraction allows OCR to be replaced later.

Test at least:

1.  Clear phone photo.
2.  Slightly angled photo.
3.  Dim room.
4.  Bright room.
5.  Slight blur.
6.  Different LeetCode problems.
7.  Problem number near top.
8.  Problem number preceded by `#`.

------------------------------------------------------------------------

# 57. Phase 7 --- Problem Number Extraction

Create:

``` kotlin
ProblemNumberExtractor
```

Input:

``` text
OCR text
```

Output:

``` kotlin
data class DetectedProblem(
    val number: Int,
    val title: String?,
    val confidence: Float
)
```

Write unit tests for multiple OCR formats.

Example:

``` text
"88. Merge Sorted Array"
-> 88

"#88 Merge Sorted Array"
-> 88

"Problem 88"
-> 88
```

Also test false positives.

------------------------------------------------------------------------

# 58. Phase 8 --- Completed Problem Store

Implement:

``` kotlin
isCompleted(problemNumber)
markCompleted(problemNumber)
getAllCompleted()
```

Acceptance test:

``` text
#88 not completed
```

then:

``` text
markCompleted(88)
```

then:

``` text
isCompleted(88) == true
```

Restart app.

Expected:

``` text
isCompleted(88) == true
```

------------------------------------------------------------------------

# 59. Phase 9 --- Explanation UI

After OCR:

``` text
Problem #88 detected.

Explain your solution:
[ multiline text field ]

[ SUBMIT ]
```

Do not call the LLM yet.

First make the UI and state transitions work.

Acceptance:

``` text
photo
 -> OCR
 -> problem detected
 -> explanation entered
 -> explanation stored in memory
```

------------------------------------------------------------------------

# 60. Phase 10 --- NIM Integration

Implement:

``` kotlin
interface ExplanationValidator {
    suspend fun validate(
        problemNumber: Int,
        title: String?,
        ocrText: String,
        explanation: String
    ): ValidationResult
}
```

Implementation:

``` kotlin
LlmExplanationValidator
```

Use a dedicated prompt builder.

Do not mix prompt construction with networking.

------------------------------------------------------------------------

# 61. Phase 11 --- LLM Response Parsing

Define:

``` kotlin
data class ValidationResult(
    val passed: Boolean,
    val confidence: Float?,
    val reason: String
)
```

Parse strictly.

If parsing fails:

``` text
ValidationResult(
    passed = false,
    reason = "Validation service returned an invalid response."
)
```

But ideally distinguish:

``` text
validation failure
```

from:

``` text
network/API failure
```

because a network failure should allow retry without implying the
explanation was wrong.

------------------------------------------------------------------------

# 62. Phase 12 --- Complete Unlock Transaction

Implement:

``` text
PASS
 ↓
completedProblems.add(problemNumber)
 ↓
credit = 300
 ↓
persist
 ↓
close verification UI
 ↓
Instagram becomes available
```

The lock overlay should disappear immediately after the service sees
that credit is positive.

------------------------------------------------------------------------

# 63. Phase 13 --- End-to-End Test

Run:

``` text
Start app
 ↓
Enable accessibility
 ↓
Instagram
 ↓
BLOCKED
 ↓
Take photo of #88
 ↓
OCR detects #88
 ↓
#88 not completed
 ↓
Explain solution
 ↓
LLM
 ↓
PASS
 ↓
#88 saved
 ↓
Credit = 5:00
 ↓
Instagram usable
 ↓
Use for 2 minutes
 ↓
Credit = 3:00
 ↓
Leave Instagram
 ↓
Credit remains ~3:00
 ↓
Return Instagram
 ↓
Use remaining time
 ↓
Credit = 0
 ↓
BLOCKED
```

This is the primary MVP acceptance test.

------------------------------------------------------------------------

# 64. Critical Edge Cases

The agent must explicitly test:

## User opens Instagram while verification is running

Expected:

``` text
Verification UI remains authoritative.
Instagram remains blocked until verification succeeds.
```

## User presses Home during verification

Expected:

``` text
Verification state remains safe.
No credit granted.
```

## User kills the app

Expected:

``` text
No accidental credit.
AccessibilityService behavior remains predictable.
```

## Network disappears during LLM request

Expected:

``` text
No credit.
No completed-problem entry.
Retry possible.
```

## LLM returns invalid JSON

Expected:

``` text
No credit.
No completed-problem entry.
Retry possible.
```

## OCR fails

Expected:

``` text
No credit.
No database modification.
Retake photo.
```

## Duplicate problem

Expected:

``` text
No LLM call.
No credit.
No database modification.
```

## LLM says FAIL

Expected:

``` text
No credit.
Problem remains available for retry.
```

## LLM says PASS

Expected:

``` text
Problem stored.
Credit = 300 seconds.
```

------------------------------------------------------------------------

# 65. Testing the Credit Logic

Write unit tests for:

``` text
CreditManager
InstagramUsageTracker
ProblemNumberExtractor
CompletedProblemStore
PromptBuilder
LLM response parser
```

Especially test:

``` text
0 + unlock = 300
100 + unlock = 300
299 + unlock = 300
300 + unlock = 300
```

Consumption:

``` text
300 - 10 = 290
290 - 100 = 190
190 - 190 = 0
0 - 10 = 0
```

Never allow:

``` text
credit < 0
credit > 300
```

------------------------------------------------------------------------

# 66. Testing the LLM

Create a local test dataset of explanations.

Example categories:

### Correct

``` text
Detailed correct approach.
```

Expected:

``` text
PASS
```

### Incorrect

``` text
Fundamentally wrong algorithm.
```

Expected:

``` text
FAIL
```

### Vague

``` text
"I use an algorithm to solve it."
```

Expected:

``` text
FAIL
```

### Very concise but correct

``` text
"Use two pointers from the end and fill nums1 backward so existing values are not overwritten."
```

Expected:

``` text
PASS
```

### Random text

``` text
"I like pizza."
```

Expected:

``` text
FAIL
```

### Code without explanation

Decide whether to accept it.

For v1, if the code clearly demonstrates understanding, it may be
accepted, but the intended input is a natural-language explanation.

------------------------------------------------------------------------

# 67. Prompt Evaluation

Do not assume the first prompt is perfect.

After the MVP works, create 30--50 example explanations:

``` text
10 clearly correct
10 clearly incorrect
10 borderline
10 vague
10 malicious/adversarial
```

Run them through the validator.

Adjust the prompt based on observed false positives and false negatives.

Do not optimize the prompt before the actual app works.

------------------------------------------------------------------------

# 68. Security and Abuse Considerations

This is a personal app, but still follow good practices.

Do not:

-   log the NIM API key
-   commit the API key
-   store photos permanently
-   store explanations unnecessarily
-   log full OCR text in production
-   log user explanations in production

For debug builds, temporary logging is acceptable.

For release builds:

``` text
No sensitive text logging.
```

------------------------------------------------------------------------

# 69. API Cost Protection

Even though the intended usage is only around 10 requests/day, implement
a local safety limit.

Example:

``` text
MAX_VALIDATION_REQUESTS_PER_DAY = 20
```

If exceeded:

``` text
AI validation limit reached today.

Try again tomorrow.
```

This prevents accidental loops from consuming the API quota.

Do not make this limit user-configurable in v1.

------------------------------------------------------------------------

# 70. Retry Policy

For transient network failures:

``` text
Attempt 1
wait 1 sec

Attempt 2
wait 2 sec

Attempt 3
wait 4 sec
```

Then fail.

Do not retry indefinitely.

Do not retry if the API returns a clearly permanent error such as
invalid API key.

------------------------------------------------------------------------

# 71. Model Configuration

Do not hardcode assumptions about one specific model's availability.

Create:

``` text
LlmConfig
```

containing:

``` text
modelName
maxOutputTokens
temperature
timeout
dailyRequestLimit
```

Suggested validation configuration:

``` text
temperature = low
maxOutputTokens = small
```

The model does not need a long response.

The desired response is only:

``` json
{
  "pass": true,
  "confidence": 0.94,
  "reason": "..."
}
```

------------------------------------------------------------------------

# 72. Offline Behavior

The app should work offline except for LLM validation.

Offline:

``` text
Instagram locked
camera works
OCR works
duplicate detection works
```

But:

``` text
LLM validation
```

cannot work without a network connection unless a local model is added
later.

Show:

``` text
Internet connection required to verify your explanation.
```

Do not grant credit when offline.

------------------------------------------------------------------------

# 73. Future Local-LLM Option

Do not implement this in v1.

But keep the validator behind an interface:

``` kotlin
interface ExplanationValidator
```

Then later:

``` text
LlmExplanationValidator
LocalExplanationValidator
```

could both implement it.

This keeps the app open to eventually using an on-device model and
becoming completely offline.

------------------------------------------------------------------------

# 74. No LeetCode API

Do not depend on LeetCode's API.

The app only needs:

``` text
problem number
title
OCR text
user explanation
```

The photographed problem itself supplies the context.

This makes the application much less fragile.

------------------------------------------------------------------------

# 75. No Local Problem Database

Do not create:

``` text
Problem(
    id,
    title,
    description,
    solution,
    difficulty
)
```

That is unnecessary for the current requirements.

The only local problem-related data is:

``` text
completed problem numbers
```

Example:

``` text
{1, 15, 20, 49, 88, 121}
```

------------------------------------------------------------------------

# 76. Final Data Model

The entire persistent application state can conceptually be:

``` kotlin
data class AppState(
    val completedProblemNumbers: Set<Int>,
    val creditSeconds: Int,
    val lastPersistedTimestamp: Long
)
```

No more is required for MVP.

------------------------------------------------------------------------

# 77. Recommended UI State Machine

Use an explicit state machine rather than many independent Boolean
flags.

Example:

``` kotlin
sealed interface VerificationState {

    data object Idle : VerificationState

    data object CapturingPhoto : VerificationState

    data object ProcessingPhoto : VerificationState

    data class ProblemDetected(
        val number: Int,
        val title: String?,
        val ocrText: String
    ) : VerificationState

    data class AskingExplanation(
        val number: Int,
        val title: String?,
        val ocrText: String
    ) : VerificationState

    data object Validating : VerificationState

    data class Failed(
        val message: String
    ) : VerificationState

    data class Success(
        val problemNumber: Int
    ) : VerificationState
}
```

This will make the application much easier for an AI coding agent to
reason about.

------------------------------------------------------------------------

# 78. Dependency Philosophy

Keep dependencies minimal.

Every dependency must have a reason.

Expected categories:

``` text
AndroidX Compose
AndroidX Lifecycle
CameraX
ML Kit Text Recognition
DataStore
Networking client
Nvidia NIM API support
```

Do not add:

``` text
Firebase
Room
Hilt
Retrofit
Coil
Navigation library
analytics SDK
crash reporting
```

unless they provide a concrete benefit required by the implementation.

A small personal application should remain small.

------------------------------------------------------------------------

# 79. Agent Instructions

The coding agent should follow these rules:

### Rule 1

Do not build the entire project in one pass.

Implement one phase at a time.

### Rule 2

After every phase:

1.  Build.
2.  Run tests.
3.  Install APK.
4.  Verify behavior.
5.  Only then continue.

### Rule 3

Do not introduce infrastructure not required by the requirements.

### Rule 4

Do not create a backend.

### Rule 5

Do not create a LeetCode database.

### Rule 6

Do not store photos permanently.

### Rule 7

Do not store explanations permanently.

### Rule 8

Do not grant credit unless the LLM explicitly returns PASS.

### Rule 9

Do not mark a problem as completed until verification succeeds.

### Rule 10

Do not let credit exceed 300 seconds.

### Rule 11

Do not let Instagram usage reduce credit while another app is
foreground.

### Rule 12

Do not expose a bypass button on the lock screen.

### Rule 13

Keep the AccessibilityService lightweight.

### Rule 14

Do not make the app dependent on an internet connection except for
LLM validation.

### Rule 15

Use interfaces around OCR, LLM validation, storage, and credit logic so
they can be unit tested.

------------------------------------------------------------------------

# 80. Definition of Done

The MVP is complete when all of the following are true:

-   [ ] App builds successfully.
-   [ ] APK installs on the target Android phone.
-   [ ] Accessibility permission can be enabled.
-   [ ] App detects Instagram.
-   [ ] Instagram is blocked when credit is zero.
-   [ ] Instagram is usable when credit is positive.
-   [ ] Instagram foreground time decreases credit.
-   [ ] Leaving Instagram pauses credit consumption.
-   [ ] Credit cannot become negative.
-   [ ] Credit cannot exceed 5 minutes.
-   [ ] Camera opens from the lock screen.
-   [ ] Gallery upload is not available.
-   [ ] Photo is processed locally using OCR.
-   [ ] Problem number can be extracted from a normal phone photo.
-   [ ] Duplicate problem numbers are rejected.
-   [ ] Explanation can be entered.
-   [ ] LLM can validate the explanation.
-   [ ] Failed validation gives zero credit.
-   [ ] Successful validation marks the problem as completed.
-   [ ] Successful validation sets the credit to exactly 5 minutes.
-   [ ] Completed problem numbers survive app restart.
-   [ ] Credit survives app restart without being increased.
-   [ ] Network failure does not grant credit.
-   [ ] Invalid LLM output does not grant credit.
-   [ ] Photos are not permanently stored.
-   [ ] Explanations are not permanently stored.
-   [ ] API key is not committed to source control.
-   [ ] There is no backend.
-   [ ] There is no cloud database.
-   [ ] There is no LeetCode API dependency.

------------------------------------------------------------------------

# 81. Final Target Architecture

The finished MVP should look like this:

``` text
                         ANDROID PHONE
                              |
             +----------------+----------------+
             |                                 |
             v                                 v
       Your Android App                    Instagram
             |                                 |
     +-------+--------+                        |
     |                |                        |
     v                v                        |
 Compose UI    AccessibilityService <----------+
     |                |
     |                v
     |          CreditManager
     |                |
     |                v
     |             DataStore
     |
     +----> CameraX
     |         |
     |         v
     |      ML Kit OCR
     |         |
     |         v
     |   Problem Number
     |         |
     |         v
     |   CompletedProblemStore
     |         |
     |      not completed
     |         |
     |         v
     |   Explanation Input
     |         |
     |         v
     |   LLM Validator
     |         |
     +---------+---------+
               |
        +------+------+
        |             |
       FAIL          PASS
        |             |
        v             v
      Retry       Save problem #
                      |
                      v
                 Set credit = 300
                      |
                      v
                 Instagram
```

------------------------------------------------------------------------

# 82. Build Priority

If time becomes limited, implement in exactly this order:

``` text
1. AccessibilityService
2. Instagram blocking
3. Credit system
4. Camera
5. OCR
6. Problem-number extraction
7. Completed-problem tracking
8. Explanation UI
9. LLM validation
10. End-to-end integration
11. Edge-case testing
12. UI polish
```

Do not spend time on visual polish before the blocking mechanism works.

The **core product is the enforcement loop**, not the dashboard.

------------------------------------------------------------------------

# 83. The One-Sentence Product Definition

The agent should use this as the project's north star:

> **Instagram is a 5-minute reward that can only be earned by
> photographing a new LeetCode problem and successfully explaining its
> solution well enough for an LLM to verify genuine understanding.**

Everything else is implementation detail.

------------------------------------------------------------------------

# 84. Official Technical References

Use current official documentation rather than relying on outdated
examples.

-   Android AccessibilityService:
    https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

-   Android AccessibilityWindowInfo:
    https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo

-   ML Kit Text Recognition:
    https://developers.google.com/ml-kit/vision/text-recognition/v2/android

-   Nvidia NIM API documentation:
    https://build.nvidia.com/
-   OpenAI API Compatibility: https://platform.openai.com/docs/api-reference
-   Before selecting an LLM model, check the current official
pricing/model documentation because free-tier model availability and
quotas can change.

------------------------------------------------------------------------

# 85. Important Product Decision

Do NOT add more features simply because they are technically possible.

The first version should remain:

``` text
Instagram
    ↓
5-minute credit
    ↓
New LeetCode problem
    ↓
Camera
    ↓
OCR
    ↓
Explanation
    ↓
LLM
    ↓
PASS
    ↓
5-minute credit
```

If that loop works reliably, the app is already doing exactly what it
was designed to do.
