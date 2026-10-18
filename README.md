# Prahari AutoFill — Native Kotlin App

React Native (Expo) version ka **native Kotlin** rewrite — Jetpack Compose UI ke saath.

## Features
- 🔐 Firebase Auth login/register
- 🔗 Prahari Connect (JWT + apni Gemini API key)
- 📝 11 screens: Audit list → Center → CDS photo → Verify → Auto-fill → Done
- 📸 Gemini Vision se CDS photo reading
- 💬 Chat se rules change ("P4 No kar do")
- 📋 10-point rulebook (Hindi remarks)
- 🔒 EncryptedSharedPreferences me secure storage

## Build (GitHub Actions)

Har `main` branch push pe APK auto-build hota hai:

1. GitHub repo banao
2. **Settings → Secrets → Actions** me `GOOGLE_SERVICES_JSON` add karo:
   - Firebase Console → Project Settings → `google-services.json` download karo
   - Poora JSON content secret me paste karo
3. Code push karo → **Actions** tab me APK milega (Artifacts)

## Local Build

```bash
# google-services.json ko app/ folder me rakho (Firebase Console se)
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/src/main/java/com/prahari/autofill/
├── MainActivity.kt          # Navigation host
├── PrahariApp.kt            # Application (DI)
├── data/
│   ├── models/Models.kt     # Audit, Center, Member, CdsData, FillAnswer
│   ├── api/
│   │   ├── PrahariApi.kt    # Prahari REST (OkHttp)
│   │   └── GeminiApi.kt     # Gemini Vision + Chat (REST)
│   ├── firebase/FirebaseManager.kt
│   ├── secure/SecureStorage.kt  # EncryptedSharedPreferences
│   └── rulebook/Rulebook.kt # 10-point logic
└── ui/
    ├── navigation/          # Routes + FillSession
    ├── theme/
    └── screens/             # 11 Compose screens
```

## Important Notes

- **Phone me InstaSafe VPN ON hona chahiye** (Prahari direct API)
- **Prahari password kabhi save nahi hota** — sirf JWT token
- **Har auditor apni Gemini API key dalega** (Prahari Connect screen)
- Firebase config: `prhari-autofill` project
