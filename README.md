<div align="center">
  <img src="assets/logo_readme.png" alt="Sera Logo" width="600" style="border-radius: 12px;"/>
  <!-- <h1>Sera</h1> -->
  <p><strong>A ruthless Android productivity app that holds Instagram hostage until you solve LeetCode problems.</strong></p>
</div>

<br/>

Sera is a local-first Android application designed to cure doom-scrolling by enforcing a strict "Proof of Work" protocol. You cannot open Instagram until you solve a LeetCode problem and explain your solution to a strict AI interviewer.

## 📸 Screenshots
<div align="center">
  <img src="assets/homescreen.jpeg" alt="Sera Homescreen" width="250" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="assets/settingsscreen.jpeg" alt="Sera Settings Screen" width="250" />
</div>

## 🚀 Usage Instructions

1. **The Lock:** An Accessibility Service runs silently in the background, monitoring your foreground apps. If you launch Instagram with 0 "Credit Time," Sera instantly intercepts the screen with an impenetrable overlay.
2. **The Key:** To earn time, you must solve a LeetCode problem. Point your phone at your computer screen and take a photo of the accepted solution.
   - **Important:** Ensure your screen shows the problem description on the left and the "Accepted" tab on the right, exactly like this:
   <br/>
   <div align="center">
     <img src="assets/leetcode.png" alt="LeetCode Formatting for OCR" width="600" />
   </div>
3. **The Validation:**
   - **Local OCR:** Sera uses on-device Google ML Kit to parse your screen, using spatial heuristics to verify the problem ID and ensure it shows "Accepted."
   - **AI Interviewer:** You must type an explanation of your algorithm (logic, time complexity, and space complexity). Sera securely sends this to an LLM (default: Nvidia NIM Llama-3.1-8B).
   - **The Reward:** If the AI validates your logic, you earn **5 minutes of Instagram time**.

## 🛠 Architecture

- **100% Kotlin & Jetpack Compose:** Built with modern Android UI paradigms.
- **Local Persistence (DataStore):** Stores user settings, credit time, and completed problem IDs (to prevent cheating by submitting the same problem twice).
- **CameraX & ML Kit:** Fast, on-device OCR. No photos are ever uploaded to the cloud.
- **OkHttp & Gson:** For lightweight, fast API calls to any OpenAI-compatible LLM endpoint.
- **OpenCode TUI Aesthetic:** A beautiful, minimal, terminal-inspired user interface.

## 💻 Installation

Sera is ready to use out of the box! You do not need to build it from source.

1. Go to the **[Releases](../../releases)** page of this repository.
2. Download the latest `Sera.apk` file and install it on your Android device.
3. Open Sera and grant the required Accessibility permissions.
4. Navigate to the **Settings** screen in the app to configure your LLM (Large Language Model).

## 🔑 AI Configuration (Bring Your Own Key)

Sera is built to be completely private and requires you to provide your own API key for an OpenAI-compatible LLM endpoint. 

**Note:** All testing and development for Sera was done using **Nvidia NIM**, as they provide free credits for a lot of different models. but it should work for any kind of api that uses a base_url, the model_name and an api_key

**How to get a free API Key via Nvidia NIM:**
1. Go to [build.nvidia.com](https://build.nvidia.com).
2. Create an account and navigate to the **Llama 3.1 8B Instruct** model.
3. Click "Get API Key" and copy it.
4. Open the Sera app, tap the Settings icon, and enter:
   - **API Key:** `nvapi-YOUR-API-KEY-HERE`
   - **Base URL:** `https://integrate.api.nvidia.com/v1`
   - **Model Name:** `meta/llama-3.1-8b-instruct`

## 📄 License
MIT License
