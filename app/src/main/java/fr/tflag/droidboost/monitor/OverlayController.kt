package fr.tflag.droidboost.monitor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

/**
 * Small draggable monitor drawn over other apps. A plain TextView keeps it light: it is redrawn every
 * second while visible.
 */
class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: TextView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 160
    }

    val isShown: Boolean get() = view != null

    /** Must run on the main thread. */
    fun show() {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val text = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding / 2)
            background = GradientDrawable().apply {
                cornerRadius = 12 * resources.displayMetrics.density
                setColor(Color.argb(170, 0, 0, 0))
            }
            setOnTouchListener(DragListener())
        }
        runCatching { windowManager.addView(text, params) }.onSuccess { view = text }
    }

    /** Must run on the main thread. */
    fun update(content: String) {
        view?.text = content
    }

    /** Must run on the main thread. */
    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
    }

    private inner class DragListener : android.view.View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(v, params) }
                }
            }
            return true
        }
    }
}
