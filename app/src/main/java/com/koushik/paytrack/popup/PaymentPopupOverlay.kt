package com.koushik.paytrack.popup

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composition
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.koushik.paytrack.data.Category
import com.koushik.paytrack.notification.PaymentInfo

object PaymentPopupOverlay {

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Adds the category-picker popup as a system overlay window over whatever app is in front. */
    fun show(context: Context, payment: PaymentInfo, onResult: (Category?) -> Unit) {
        if (!canDrawOverlays(context)) return

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.performRestore()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        var composeView: AbstractComposeView? = null

        fun dismiss(result: Category?) {
            val view = composeView ?: return
            composeView = null
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            runCatching { windowManager.removeView(view) }
            onResult(result)
        }

        val view = object : AbstractComposeView(context) {
            @androidx.compose.runtime.Composable
            override fun Content() {
                PaymentPopupContent(
                    payment = payment,
                    onCategorySelected = { category -> dismiss(category) },
                    onSkip = { dismiss(null) },
                )
            }

            override fun getAccessibilityClassName(): CharSequence = "PaymentPopupOverlay"
        }
        composeView = view

        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        runCatching { windowManager.addView(view, params) }
            .onFailure { onResult(null) }
    }
}
