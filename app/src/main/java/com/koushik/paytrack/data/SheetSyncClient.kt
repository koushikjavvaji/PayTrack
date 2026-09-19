package com.koushik.paytrack.data

import android.util.Log
import com.koushik.paytrack.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends one category's delta to the Apps Script web app bound to the Google Sheet (see
 * apps-script/Code.gs). The script does the read-add-write itself and recomputes the
 * month-to-date total from the sheet's own history — the phone never needs that history.
 * The endpoint responds via a redirect that isn't worth following — the write already happens
 * on the initial request — so any 2xx/3xx is success.
 */
object SheetSyncClient {
    private const val TAG = "PayTrack"

    /** Returns true if the push succeeded (or sync isn't configured, so there's nothing to retry). */
    fun pushDelta(displayDate: String, category: Category, amount: Double): Boolean {
        if (BuildConfig.SHEET_SYNC_URL.isBlank()) return true

        return try {
            val payload = JSONObject().apply {
                put("displayDate", displayDate)
                put("category", category.sheetKey)
                put("amount", amount)
            }

            val connection = URL(BuildConfig.SHEET_SYNC_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = false
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(payload.toString().toByteArray()) }
            }

            val code = connection.responseCode
            connection.disconnect()

            val success = code in 200..399
            if (!success) {
                Log.w(TAG, "Sheet sync failed for $displayDate/${category.sheetKey}: HTTP $code")
            }
            success
        } catch (e: Exception) {
            Log.w(TAG, "Sheet sync error for $displayDate/${category.sheetKey}", e)
            false
        }
    }
}
