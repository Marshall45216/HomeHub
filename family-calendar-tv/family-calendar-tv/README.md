# Family Calendar TV

A native Fire TV app that shows your family's combined Outlook/Microsoft 365
calendar, styled after the HomeHub mockup: today hero panel, weekly menu
strip, weather strip, per-person filtering, countdown chips, voice quick-add,
and an idle-mode "Next 24 Hours" screensaver.

**Right now the app runs entirely on sample data** so you can see and tweak
the UI immediately. The sections below are what's left to connect it to your
real family calendar and get it onto your Firestick.

## 1. What you'll need

- **Android Studio** (free) on a Windows/Mac/Linux computer — this project can't
  be compiled here; it needs to be built on your machine.
- A **Microsoft Azure account** (free tier is fine) to register the app so it's
  allowed to read your family's Outlook calendars.
- Your **Firestick** and computer on the same Wi-Fi network, for sideloading.

## 2. Azure app registration (one-time, ~10 minutes)

This is what lets the app ask Microsoft for permission to read calendars.

1. Go to https://portal.azure.com → **Azure Active Directory** → **App registrations** → **New registration**.
2. Name it anything (e.g. "Family Calendar TV").
3. Under **Supported account types**, choose "Accounts in any organizational
   directory and personal Microsoft accounts" (so personal Outlook.com
   accounts work too, not just work/school ones).
4. Skip Redirect URI for now — click **Register**.
5. Copy the **Application (client) ID** shown on the overview page.
6. Paste it into `app/src/main/res/raw/auth_config.json`, replacing
   `PASTE_YOUR_AZURE_APP_CLIENT_ID_HERE`.
7. Under **API permissions**, add `Calendars.ReadWrite` and `User.Read`
   (Microsoft Graph, delegated permissions).
8. Under **Authentication** → **Add a platform** → **Mobile and desktop
   applications**, add redirect URI: `msauth://com.familycal.tv/YOUR_HASH`
   — you'll generate the real hash in step 3 below and come back to update
   this.

## 3. Redirect URI signature hash

MSAL needs your app's signing certificate hash in the redirect URI. In
Android Studio's terminal, from the project root:

```
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64
```

Take the output string and:
- Replace `YOUR_BASE64_SIGNATURE_HASH` in `AndroidManifest.xml`
- Replace `YOUR_BASE64_SIGNATURE_HASH` in `res/raw/auth_config.json`
- Update the redirect URI you added in Azure step 8 to match

## 4. Weather setup

The weather strip uses Open-Meteo (no API key needed). In `MainActivity.kt`,
replace the sample forecast call with:

```kotlin
val weatherRepo = remember { WeatherRepository() }
var forecast by remember { mutableStateOf<List<DayForecast>>(emptyList()) }
LaunchedEffect(Unit) {
    forecast = weatherRepo.getWeekForecast(lat = YOUR_LAT, lon = YOUR_LON)
}
```

Find your lat/lon by searching "[your city] latitude longitude."

## 5. Connecting real family calendars

In `MainActivity.kt`, replace `sampleFamilyMembers()` and `sampleEvents()`
with a sign-in flow: for each family member, call
`AuthManager.signInFamilyMember()`, display the resulting device code on
screen (there's a hook for this — `onCodeReady`), and once signed in call
`GraphRepository.getCombinedFamilyEvents()` with all their tokens to get the
merged event list. Re-run that fetch on a timer (e.g. every 15 minutes) to
keep the display current.

## 6. Building and sideloading to your Firestick

1. Open this project folder in Android Studio and let it sync (first sync
   downloads dependencies — takes a few minutes).
2. **Build → Build Bundle(s)/APK(s) → Build APK(s)**. The APK lands in
   `app/build/outputs/apk/debug/`.
3. On your Firestick: **Settings → My Fire TV → Developer Options** → turn on
   **ADB Debugging** and **Apps from Unknown Sources**.
4. Find your Firestick's IP: **Settings → My Fire TV → About → Network**.
5. From your computer's terminal:
   ```
   adb connect <firestick-ip>:5555
   adb install app-debug.apk
   ```
6. The app appears on your Fire TV home screen as "Family Calendar" — press
   the remote's mic button and say **"Alexa, open Family Calendar"** to
   launch it by voice.

## 7. Before you sideload for daily use

- Swap the placeholder banner (`res/drawable/app_banner.xml`) and launcher
  icon (`res/drawable/ic_launcher_*.xml`) for real artwork — a 320×180 PNG
  banner and proper adaptive icon look much better on the Fire TV home row.
- The mic button in the voice quick-add bar is currently a UI stub. Wiring it
  to `SpeechRecognizer` and parsing the phrase into a calendar event is the
  next step — flagged with a comment in `MainActivity.kt`.

## Project structure

```
app/src/main/java/com/familycal/tv/
  MainActivity.kt          — entry point, sample data, idle-mode timer
  auth/AuthManager.kt       — Microsoft sign-in (device code flow)
  graph/GraphRepository.kt  — fetches & merges family calendars
  graph/WeatherRepository.kt— Open-Meteo weekly forecast
  model/FamilyModels.kt     — data models
  ui/CalendarScreen.kt      — main dashboard (hero, avatars, countdowns, weather, voice bar)
  ui/IdleAmbientScreen.kt   — "Next 24 Hours" screensaver
  ui/theme/Theme.kt         — HomeHub dark navy/purple theme
```
