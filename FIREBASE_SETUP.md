# Firebase Setup for ESMERY

1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app with package name `com.aistudio.sanctuary.xkmdzs`
3. Download `google-services.json` and place it in `app/google-services.json`
4. Uncomment `alias(libs.plugins.google.services)` in `app/build.gradle.kts`
5. Set Supabase edge function secrets:
   - `FCM_PROJECT_ID`
   - `FCM_CLIENT_EMAIL`
   - `FCM_PRIVATE_KEY`

Push notifications use FCM HTTP v1 via the `safety-automation` edge function.
