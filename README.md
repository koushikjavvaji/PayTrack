# PayTrack

Android app that reads GPay/Paytm payment notifications and helps you categorize spending, with no backend.

## Status: V1

- `NotificationListenerService` detects GPay/Paytm payment notifications and logs amount/merchant to Logcat.
- Compose UI to grant notification access.

Not yet implemented: category popup, local storage (Room), Google Sheets/Excel export.

## Running

```
./gradlew :app:installDebug
```

Then on the device: Settings → PayTrack → grant notification access (or use the in-app button), make a GPay/Paytm payment, and watch Logcat filtered on tag `PayTrack`:

```
adb logcat -s PayTrack
```
