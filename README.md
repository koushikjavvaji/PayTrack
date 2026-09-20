# PayTrack

Android app that detects GPay/Paytm payments (plus bank SMS/email debit alerts as a fallback), asks you to categorize each one via a popup, tracks monthly budgets per category, and logs everything locally and to a Google Sheet — no backend server.

## How it works

- `PaymentNotificationListenerService` listens for notifications from:
  - **Payment apps** — Google Pay, Paytm (parsed directly).
  - **Messages** — bank debit SMS forwarded through Google Messages.
  - **Gmail** — bank debit email alerts.
- A detected payment (amount + merchant) triggers a system-overlay popup asking you to pick a category (Breakfast, Lunch, Dinner, Snacks, Travel, Other Expenses).
- The same payment is often reported by both the payment app and the bank SMS/email — a short dedup window drops the duplicate.
- If picking a category would push that category over its monthly budget, the popup shakes, flashes red, and gives a distinct double-buzz haptic before recording the payment — an immediate, in-the-moment nudge instead of a number you'd only notice later.
- Each categorized transaction is saved locally in a Room database (transactions + daily summaries) and its delta is pushed to a Google Sheet via an Apps Script web app.
- If the sheet push fails (e.g. no network), the delta is queued locally and retried automatically the next time a payment comes in, the listener reconnects, or the app is opened.
- The home screen shows a "spent this month" hero card, per-category monthly budgets with progress bars, recent daily totals, and recent transactions — tap one to fix a miscategorized entry, or swipe it away to delete (with an undo snackbar). You can also add a payment that was missed manually.

## Project structure

- `app/` — the Android app (Kotlin, Jetpack Compose, Room).
- `apps-script/Code.gs` — Google Apps Script web app deployed to the target Google Sheet. It receives one category's delta per request, does a read-add-write on the matching date row, and recomputes that row's total and the month-to-date running total from the sheet's own history.

Budgets are local-only (set per category from the home screen's "Edit" button) — they're never sent to the sheet, which only ever receives `{displayDate, category, amount}` deltas.

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
6. Optionally, tap **Edit** on the home screen's Budgets card to set a monthly limit per category.

## Debugging

```
adb logcat -s PayTrack
```

Logs notification detection, parsed payment info, and sheet sync failures/retries.

## License

MIT — see [LICENSE](LICENSE).
