# PayTrack

Android app that detects GPay/Paytm payments (plus bank SMS/email debit alerts as a fallback), asks you to categorize each one via a popup, and logs everything locally and to a Google Sheet — no backend server.

## How it works

- `PaymentNotificationListenerService` listens for notifications from:
  - **Payment apps** — Google Pay, Paytm (parsed directly).
  - **Messages** — bank debit SMS forwarded through Google Messages.
  - **Gmail** — bank debit email alerts.
- A detected payment (amount + merchant) triggers a system-overlay popup asking you to pick a category (Breakfast, Lunch, Dinner, Snacks, Travel, Other Expenses).
- The same payment is often reported by both the payment app and the bank SMS/email — a short dedup window drops the duplicate.
- Each categorized transaction is saved locally in a Room database (transactions + daily summaries) and its delta is pushed to a Google Sheet via an Apps Script web app.
- If the sheet push fails (e.g. no network), the delta is queued locally and retried automatically the next time a payment comes in, the listener reconnects, or the app is opened.
- The home screen shows recent daily totals and recent transactions, lets you tap a transaction to fix a miscategorized entry, and lets you manually add a payment that was missed.

## Project structure

- `app/` — the Android app (Kotlin, Jetpack Compose, Room).
- `apps-script/Code.gs` — Google Apps Script web app deployed to the target Google Sheet. It receives one category's delta per request, does a read-add-write on the matching date row, and recomputes that row's total and the month-to-date running total from the sheet's own history.

## Setup

1. Deploy `apps-script/Code.gs` as a web app (execute as yourself, accessible to anyone with the link) against your Google Sheet, and note the deployment URL.
2. In `local.properties`, set:
   ```
   sheetSyncUrl=<your Apps Script web app URL>
   ```
   If left blank, sheet sync is skipped and the app works purely locally.
3. Build and install:
   ```
   ./gradlew :app:installDebug
   ```
4. On the device, open PayTrack and grant:
   - **Notification access** (Settings → PayTrack), so the listener can read payment/SMS/email notifications.
   - **Overlay permission**, so the category popup can be drawn over other apps.
5. Make a GPay/Paytm payment (or receive a bank debit SMS/email) and pick a category in the popup that appears.

## Debugging

```
adb logcat -s PayTrack
```

Logs notification detection, parsed payment info, and sheet sync failures/retries.
