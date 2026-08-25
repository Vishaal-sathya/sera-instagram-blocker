# LeetCodeGate 🛑⏳

LeetCodeGate is a ruthless, local-first Android app designed to cure doom-scrolling by holding your Instagram access hostage until you solve a LeetCode problem.

## How it Works
1. **The Lock:** An Accessibility Service constantly monitors if you open Instagram. If you have 0 "Credit Time", it instantly throws a full-screen overlay over the app.
2. **The Key:** To earn time, you must solve a LeetCode problem. Take a photo of your computer screen showing the problem.
3. **The Validation:** 
   - **Local OCR:** The app uses on-device Google ML Kit to read your screen and parse exactly which problem you are looking at (using spatial heuristics to find the title/ID).
   - **LLM Grader:** You must type out a >30 character explanation of your solution. The app securely sends this explanation + the OCR context to a local or cloud LLM (default: Nvidia NIM Llama 3.1 8B).
   - **The Reward:** If the LLM validates your logic is sound, you earn 5 minutes of Instagram time!

## Architecture
- **100% Kotlin & Jetpack Compose**
- **DataStore:** Stores user settings, remaining credit time, and a set of completed problem IDs to prevent cheating.
- **CameraX & ML Kit:** For fast, local, on-device OCR without uploading photos to the cloud.
- **OkHttp:** For lightweight API calls to OpenAI-compatible LLM endpoints.

## Local Development & Setup

This repository is built to be credential-free. You must provide your own LLM API key. 

1. Clone the repository.
2. Create a `local.properties` file in the root directory.
3. Add your Nvidia NIM API key (or OpenAI key):
   ```properties
   NIM_API_KEY=nvapi-YOUR-API-KEY-HERE
   NIM_BASE_URL=https://integrate.api.nvidia.com/v1
   ```
4. Build and install via Android Studio.

## How to get a free Nvidia NIM API Key
Nvidia NIM provides extremely generous free credits for developers.
1. Go to [build.nvidia.com](https://build.nvidia.com).
2. Create an account and navigate to any model (e.g., Llama 3.1 8B Instruct).
3. Click "Get API Key" and copy it into your `local.properties` or directly into the app's Settings screen!

## License
MIT License
