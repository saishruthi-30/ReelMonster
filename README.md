# Reel Monster 👾 (Android, offline, no login)

A cute monster bubble that floats over Instagram and YouTube and counts your reels.

## Get the APK (no Android Studio needed)

### Option A: GitHub builds it for you (easiest)
1. Create a new empty repository on github.com.
2. Upload ALL files from this folder (keep the `.github` folder) and commit to `main`.
3. Open the **Actions** tab > **Build APK** > the latest run (or press "Run workflow").
4. When it turns green, download **ReelMonster-APK** at the bottom of the run page, unzip it and you get `app-debug.apk`.
5. Send the APK to your phone and install it (allow "Install unknown apps" when asked).

### Option B: command line on your own computer
Install JDK 17, the Android command-line tools and Gradle 8.5, then:
```
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
gradle assembleDebug
```
The APK appears in `app/build/outputs/apk/debug/app-debug.apk`.

## First run on the phone
1. Answer the questions (who you are, age) and pick Manual or Automatic.
2. Home screen > **Quick setup**: allow "Display over other apps" (and Notifications).
3. Automatic mode only: switch on **Reel Monster auto counter** in Accessibility.
   If the switch is greyed out: Settings > Apps > Reel Monster > ⋮ > **Allow restricted settings**.
4. Tap **Wake the monster**, then open Instagram or YouTube.

## Checking Level 2 (automatic) accuracy
Watch 20+ reels, count them yourself, and compare. Settings > "Auto-count log" shows what the counter saw.
Instagram and YouTube change their screens often, so if it is off by more than 2, send me a screenshot of that log and I will tune it.

## If you cannot see the hidden `.github` folder
Use the visible file `build-apk-workflow.yml` instead. On GitHub: **Add file > Create new file**, type the name
`.github/workflows/build.yml` (typing the slashes creates the folders), paste in the contents of `build-apk-workflow.yml`, and commit.
