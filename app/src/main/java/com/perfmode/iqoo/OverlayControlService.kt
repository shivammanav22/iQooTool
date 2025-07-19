package com.perfmode.iqoo

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.perfmode.iqoo.model.Feature
import com.perfmode.iqoo.util.DataStoreManager
import com.perfmode.iqoo.util.FeatureRepository
import com.perfmode.iqoo.util.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.text.method.ScrollingMovementMethod
import android.text.InputType
import kotlinx.coroutines.isActive
import android.view.MotionEvent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.app.NotificationCompat
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Switch


// Enum for different overlay sections
enum class OverlaySection {
    FEATURES,
    STATS,
    TERMINAL
}

/**
 * Extension function for Context to convert Int DP values to Float PX values.
 * This function takes an Int 'dp' value and returns its equivalent in pixels (Float).
 * It's defined at the top-level of this file, making it a "local" extension
 * function accessible within this file's scope.
 */
private fun Context.dpToPx(dp: Int): Float {
    return dp * resources.displayMetrics.density
}

/**
 * A service that displays a multi-layered overlay with CPU/Memory status, a terminal,
 * and selected feature toggles, built using traditional Android Views.
 * Requires SYSTEM_ALERT_WINDOW permission.
 */
class OverlayControlService : Service() {

    private val TAG = "OverlayControlService"
    private lateinit var windowManager: WindowManager
    private lateinit var overlayRootView: LinearLayout // The root view of the overlay
    private lateinit var serviceScope: CoroutineScope

    // State for feature toggles, updated from DataStore
    private var currentFeatureStates: Map<String, Boolean> = emptyMap()
    private var allAvailableFeatures: List<Feature> = emptyList()


    // Overlay state variables
    private var isOverlayExpanded: Boolean = true // True when showing all sections, false when collapsed
    private lateinit var collapseButton: TextView

    // Store the WindowManager.LayoutParams for the overlay window
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var currentOverlaySection: OverlaySection = OverlaySection.FEATURES

    // --- Dragging variables ---
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    // --- UI ELEMENTS FOR MULTI-LAYERED OVERLAY ---
    private lateinit var topBarLayout: LinearLayout // Contains collapse button
    private lateinit var navigationBarLayout: LinearLayout // Contains tab buttons
    private lateinit var contentFrameLayout: LinearLayout // Holds the active content section (Stats, Terminal or Features)

    // STATS Section
    private lateinit var statsLayout: LinearLayout
    private lateinit var cpuUsageTextView: TextView
    private lateinit var cpuFreqTextView: TextView
    private lateinit var memUsageTextView: TextView
    private lateinit var memFreqTextView: TextView
    private lateinit var batteryLevelTextView: TextView
    private var lastCpuTotal: Long = 0
    private var lastCpuIdle: Long = 0
    private var statsUpdateJob: Job? = null

    // Navigation Buttons
    private lateinit var navBtnFeatures: Button
    private lateinit var navBtnStats: Button
    private lateinit var navBtnTerminal: Button

    // TERMINAL Section
    private lateinit var terminalLayout: LinearLayout
    private lateinit var terminalInput: EditText
    private lateinit var terminalExecuteBtn: Button
    private lateinit var terminalClearBtn: Button
    private lateinit var terminalOutput: TextView
    private lateinit var terminalScrollView: ScrollView

    // FEATURES Section
    private lateinit var featuresLayout: LinearLayout
    private var featureListUpdateJob: Job? = null

    // --- Theming Colors (now directly android.graphics.Color int values) ---
    private val overlayBackgroundColor = Color.parseColor("#AA212121")
    private val primaryTextColor = Color.WHITE
    private val secondaryTextColor = Color.parseColor("#CCCCCC")
    private val accentColor = Color.parseColor("#4CAF50")
    private val inputBackgroundColor = Color.parseColor("#33FFFFFF")

    private val navButtonSelectedBg = Color.parseColor("#334CAF50")
    private val navButtonUnselectedBg = Color.TRANSPARENT


    private val NOTIFICATION_CHANNEL_ID = "OverlayServiceChannel"
    private val NOTIFICATION_ID = 101

    // Battery BroadcastReceiver
    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val batteryPct = (level / scale.toFloat() * 100).toInt()
                    batteryLevelTextView.text = "Battery: $batteryPct%"
                    Log.d(TAG, "Battery Level Updated: $batteryPct%")
                }
            }
        }
    }


    companion object {
        const val ACTION_SHOW_OVERLAY = "com.perfmode.iqoo.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.perfmode.iqoo.ACTION_HIDE_OVERLAY"
        const val ACTION_UPDATE_OVERLAY_STATE = "com.perfmode.iqoo.ACTION_UPDATE_OVERLAY_STATE"
        const val EXTRA_TOGGLE_STATE_MAP = "com.perfmode.iqoo.EXTRA_TOGGLE_STATE_MAP"
        const val EXTRA_OVERLAY_FEATURE_TITLES = "com.perfmode.iqoo.EXTRA_OVERLAY_FEATURE_TITLES"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service: onCreate called.")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        serviceScope = CoroutineScope(Dispatchers.Main)

        // --- Foreground Service Setup ---
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("iQOO Tweaks Overlay")
            .setContentText("Overlay is active. Tap to open app.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Service: Started as foreground service.")

        setupOverlayViews()
        Log.d(TAG, "Service: All overlay views initialized.")

        // Register Battery Receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
        Log.d(TAG, "BatteryReceiver registered.")

        // Start initial data fetching for features and stats
        serviceScope.launch {
            FeatureRepository.getAllFeaturesFlow(applicationContext).collect { features ->
                allAvailableFeatures = features
                Log.d(TAG, "All available features updated: ${features.size} features.")
                updateOverlayContent()
            }
        }

        serviceScope.launch {
            DataStoreManager.getFeatureTogglesFlow(applicationContext).collect { toggles ->
                currentFeatureStates = toggles
                Log.d(TAG, "All feature states updated: $currentFeatureStates")
                updateOverlayContent()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service: onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                Log.d(TAG, "Service: ACTION_SHOW_OVERLAY received.")
                showOverlay()
                updateOverlayContent()
            }
            ACTION_HIDE_OVERLAY -> {
                Log.d(TAG, "Service: ACTION_HIDE_OVERLAY received.")
                hideOverlay()
            }
            ACTION_UPDATE_OVERLAY_STATE -> {
                Log.d(TAG, "Service: ACTION_UPDATE_OVERLAY_STATE received. Triggering content update.")
                updateOverlayContent()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service: onDestroy called. Cleaning up.")
        statsUpdateJob?.cancel()
        featureListUpdateJob?.cancel()
        hideOverlay()
        unregisterReceiver(batteryReceiver)
        Log.d(TAG, "BatteryReceiver unregistered.")
        stopForeground(true)
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for iQOO Tweaks overlay service notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Sets up the entire overlay view hierarchy programmatically.
     */
    @SuppressLint("SetTextI18n")
    private fun setupOverlayViews() {
        // --- Root Overlay View ---
        overlayRootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(overlayBackgroundColor)
            setPadding(dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt())

            val shapeDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(overlayBackgroundColor)
                cornerRadius = dpToPx(8)
            }
            background = shapeDrawable

            // --- Draggable functionality for the entire overlay ---
            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    if (overlayLayoutParams == null) return false

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = overlayLayoutParams!!.x
                            initialY = overlayLayoutParams!!.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY

                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv())
                            windowManager.updateViewLayout(overlayRootView, overlayLayoutParams)
                            Log.d(TAG, "Overlay ACTION_DOWN: Flags changed for dragging. New flags: ${overlayLayoutParams!!.flags}")
                        }
                        MotionEvent.ACTION_MOVE -> {
                            Log.d(TAG, "Overlay ACTION_MOVE: rawX=${event.rawX}, initialTouchX=${initialTouchX}, rawY=${event.rawY}, initialTouchY=${initialTouchY}")
                            Log.d(TAG, "Overlay ACTION_MOVE: deltaX=${event.rawX - initialTouchX}, deltaY=${event.rawY - initialTouchY}")
                            Log.d(TAG, "Overlay ACTION_MOVE: initialX=${initialX}, initialY=${initialY}")

                            // FIX FOR INVERTED MOVEMENT: Negate the delta X and Y
                            overlayLayoutParams!!.x = initialX - (event.rawX - initialTouchX).toInt()
                            overlayLayoutParams!!.y = initialY - (event.rawY - initialTouchY).toInt()

                            windowManager.updateViewLayout(overlayRootView, overlayLayoutParams)
                            Log.d(TAG, "Overlay ACTION_MOVE: newX=${overlayLayoutParams!!.x}, newY=${overlayLayoutParams!!.y}")
                        }
                        MotionEvent.ACTION_UP -> {
                            // Revert flags after dragging is complete
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                            windowManager.updateViewLayout(overlayRootView, overlayLayoutParams)
                            Log.d(TAG, "Overlay ACTION_UP: Flags reverted after dragging. New flags: ${overlayLayoutParams!!.flags}")
                        }
                    }
                    return true
                }
            })
        }

        // --- Row 1: Top Bar (Collapse Button) ---
        topBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
            setPadding(0, 0, 0, dpToPx(2).toInt())
        }

        collapseButton = TextView(this).apply {
            text = "▲"
            textSize = 12f
            setTextColor(primaryTextColor)
            setPadding(dpToPx(4).toInt(), dpToPx(1).toInt(), dpToPx(4).toInt(), dpToPx(1).toInt())
            gravity = Gravity.CENTER
            setOnClickListener {
                isOverlayExpanded = !isOverlayExpanded
                updateOverlayContent()
                Log.d(TAG, "Service: Collapse button clicked. isOverlayExpanded: $isOverlayExpanded")
            }
        }
        topBarLayout.addView(collapseButton)
        overlayRootView.addView(topBarLayout)

        // --- Row 2: Navigation Bar (Tab Buttons - Visible when expanded) ---
        navigationBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setPadding(0, dpToPx(4).toInt(), 0, dpToPx(4).toInt())
            visibility = View.GONE

            navBtnFeatures = Button(this@OverlayControlService).apply {
                text = "Features"
                textSize = 10f
                setPadding(dpToPx(6).toInt(), dpToPx(3).toInt(), dpToPx(6).toInt(), dpToPx(3).toInt())
                setOnClickListener {
                    currentOverlaySection = OverlaySection.FEATURES
                    updateOverlayContent()
                }
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(secondaryTextColor)
            }
            addView(navBtnFeatures)

            navBtnStats = Button(this@OverlayControlService).apply {
                text = "Stats"
                textSize = 10f
                setPadding(dpToPx(6).toInt(), dpToPx(3).toInt(), dpToPx(6).toInt(), dpToPx(3).toInt())
                setOnClickListener {
                    currentOverlaySection = OverlaySection.STATS
                    updateOverlayContent()
                }
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(secondaryTextColor)
            }
            addView(navBtnStats)

            navBtnTerminal = Button(this@OverlayControlService).apply {
                text = "Terminal"
                textSize = 10f
                setPadding(dpToPx(6).toInt(), dpToPx(3).toInt(), dpToPx(6).toInt(), dpToPx(3).toInt())
                setOnClickListener {
                    currentOverlaySection = OverlaySection.TERMINAL
                    updateOverlayContent()
                }
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(secondaryTextColor)
            }
            addView(navBtnTerminal)
        }
        overlayRootView.addView(navigationBarLayout)

        // --- Row 3: Content Frame (holds dynamic section layouts - Visible when expanded) ---
        contentFrameLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt())
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#22FFFFFF"))
                cornerRadius = dpToPx(6)
            }
        }
        overlayRootView.addView(contentFrameLayout)

        // --- STATS Layout (Child of contentFrameLayout) ---
        statsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 0)
            visibility = View.GONE
            startStatsUpdates()
        }

        cpuUsageTextView = TextView(this@OverlayControlService).apply {
            text = "CPU: --%"
            textSize = 12f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(2).toInt())
        }
        statsLayout.addView(cpuUsageTextView)

        cpuFreqTextView = TextView(this@OverlayControlService).apply {
            text = "CPU Freq: -- MHz"
            textSize = 12f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(2).toInt())
        }
        statsLayout.addView(cpuFreqTextView)

        memUsageTextView = TextView(this@OverlayControlService).apply {
            text = "RAM: -- MB / -- MB"
            textSize = 12f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(2).toInt())
        }
        statsLayout.addView(memUsageTextView)

        memFreqTextView = TextView(this@OverlayControlService).apply {
            text = "Mem Freq: -- MHz"
            textSize = 12f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, dpToPx(2).toInt())
        }
        statsLayout.addView(memFreqTextView)

        batteryLevelTextView = TextView(this@OverlayControlService).apply {
            text = "Battery: --%"
            textSize = 12f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(2).toInt())
        }
        statsLayout.addView(batteryLevelTextView)

        contentFrameLayout.addView(statsLayout)

        // --- TERMINAL Layout (Child of contentFrameLayout) ---
        terminalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 0)
            visibility = View.GONE

            terminalInput = EditText(this@OverlayControlService).apply {
                hint = "Enter command..."
                setTextColor(primaryTextColor)
                setHintTextColor(secondaryTextColor)
                setBackgroundColor(inputBackgroundColor)
                setPadding(dpToPx(6).toInt(), dpToPx(6).toInt(), dpToPx(6).toInt(), dpToPx(6).toInt())
                maxLines = 1
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    if (overlayLayoutParams != null) {
                        if (hasFocus) {
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv())
                            Log.d(TAG, "Terminal input focused. Overlay flags: ${overlayLayoutParams!!.flags}")
                        } else {
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                            overlayLayoutParams!!.flags = (overlayLayoutParams!!.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                            Log.d(TAG, "Terminal input unfocused. Overlay flags: ${overlayLayoutParams!!.flags}")
                        }
                        windowManager.updateViewLayout(overlayRootView, overlayLayoutParams)
                    }
                }
            }
            addView(terminalInput)

            val terminalButtonsLayout = LinearLayout(this@OverlayControlService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(4).toInt()
                }
                gravity = Gravity.END
            }

            terminalExecuteBtn = Button(this@OverlayControlService).apply {
                text = "Execute"
                textSize = 10f
                setPadding(dpToPx(6).toInt(), dpToPx(3).toInt(), dpToPx(6).toInt(), dpToPx(3).toInt())
                setOnClickListener { executeTerminalCommand() }
                setBackgroundColor(accentColor)
                setTextColor(Color.WHITE)
            }
            terminalButtonsLayout.addView(terminalExecuteBtn)

            terminalButtonsLayout.addView(View(this@OverlayControlService).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(6).toInt(), 1)
            })

            terminalClearBtn = Button(this@OverlayControlService).apply {
                text = "Clear"
                textSize = 10f
                setPadding(dpToPx(6).toInt(), dpToPx(3).toInt(), dpToPx(6).toInt(), dpToPx(3).toInt())
                setOnClickListener { terminalOutput.text = "Terminal Output:\n" }
                setBackgroundColor(accentColor)
                setTextColor(Color.WHITE)
            }
            terminalButtonsLayout.addView(terminalClearBtn)

            addView(terminalButtonsLayout)

            terminalScrollView = ScrollView(this@OverlayControlService).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(100).toInt()
                ).apply {
                    topMargin = dpToPx(4).toInt()
                }
            }
            terminalOutput = TextView(this@OverlayControlService).apply {
                text = "Terminal Output:\n"
                textSize = 9f
                setTextColor(accentColor)
                setBackgroundColor(inputBackgroundColor)
                setPadding(dpToPx(6).toInt(), dpToPx(6).toInt(), dpToPx(6).toInt(), dpToPx(6).toInt())
                movementMethod = ScrollingMovementMethod()
            }
            terminalScrollView.addView(terminalOutput)
            addView(terminalScrollView)
        }
        contentFrameLayout.addView(terminalLayout)

        // --- FEATURES Layout (Child of contentFrameLayout) ---
        featuresLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt())
            visibility = View.GONE
        }
        contentFrameLayout.addView(featuresLayout)
    }

    private fun showOverlay() {
        if (overlayRootView.parent == null) {
            Log.d(TAG, "Service: Attempting to add overlay to window.")
            val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            overlayLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dpToPx(10).toInt()
                y = dpToPx(100).toInt()
            }

            try {
                windowManager.addView(overlayRootView, overlayLayoutParams)
                Log.d(TAG, "Service: Overlay added to window successfully.")
                isOverlayExpanded = true
                updateOverlayContent()
            } catch (e: Exception) {
                Log.e(TAG, "Service: FAILED to add overlay to window! Exception: ${e.message}", e)
                Toast.makeText(this, "Failed to show overlay. Check 'Display over other apps' permission.", Toast.LENGTH_LONG).show()
                stopSelf()
            }
        } else {
            Log.d(TAG, "Service: Overlay already visible, not re-adding. Just updating content.")
            updateOverlayContent()
        }
    }

    private fun hideOverlay() {
        if (overlayRootView.parent != null) {
            try {
                statsUpdateJob?.cancel()
                featureListUpdateJob?.cancel()
                windowManager.removeView(overlayRootView)
                overlayLayoutParams = null
                Log.d(TAG, "Service: Overlay removed from window.")
            } catch (e: Exception) {
                Log.e(TAG, "Service: Error removing overlay from window: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Service: Overlay not currently visible, no need to hide.")
        }
    }

    /**
     * Updates the visibility of sections based on expansion state and selected section.
     * Also updates the overlay window's layout parameters.
     */
    private fun updateOverlayContent() {
        Log.d(TAG, "updateOverlayContent: isOverlayExpanded=$isOverlayExpanded, currentOverlaySection=$currentOverlaySection")

        collapseButton.text = if (isOverlayExpanded) "▲" else "▼"

        if (isOverlayExpanded) {
            navigationBarLayout.visibility = View.VISIBLE
            contentFrameLayout.visibility = View.VISIBLE
            Log.d(TAG, "updateOverlayContent: navigationBarLayout VISIBLE, contentFrameLayout VISIBLE")

            // Hide all dynamic content layouts first
            statsLayout.visibility = View.GONE
            terminalLayout.visibility = View.GONE
            featuresLayout.visibility = View.GONE
            Log.d(TAG, "updateOverlayContent: All dynamic content layouts GONE initially.")

            // Then show the selected one
            when (currentOverlaySection) {
                OverlaySection.FEATURES -> {
                    featuresLayout.visibility = View.VISIBLE
                    updateFeatureListContent()
                    Log.d(TAG, "updateOverlayContent: Showing FEATURES layout.")
                }
                OverlaySection.STATS -> {
                    statsLayout.visibility = View.VISIBLE
                    Log.d(TAG, "updateOverlayContent: 'Stats' tab selected. Showing statsLayout.")
                }
                OverlaySection.TERMINAL -> {
                    terminalLayout.visibility = View.VISIBLE
                    Log.d(TAG, "updateOverlayContent: Showing TERMINAL layout.")
                }
            }
            // Update button colors based on selected section
            navBtnFeatures.setTextColor(if (currentOverlaySection == OverlaySection.FEATURES) accentColor else secondaryTextColor)
            navBtnStats.setTextColor(if (currentOverlaySection == OverlaySection.STATS) accentColor else secondaryTextColor)
            navBtnTerminal.setTextColor(if (currentOverlaySection == OverlaySection.TERMINAL) accentColor else secondaryTextColor)

            // Make selected button background visible
            navBtnFeatures.background = if (currentOverlaySection == OverlaySection.FEATURES) createRoundedRectDrawable(navButtonSelectedBg) else null
            navBtnStats.background = if (currentOverlaySection == OverlaySection.STATS) createRoundedRectDrawable(navButtonSelectedBg) else null
            navBtnTerminal.background = if (currentOverlaySection == OverlaySection.TERMINAL) createRoundedRectDrawable(navButtonSelectedBg) else null


        } else { // When collapsed
            navigationBarLayout.visibility = View.GONE
            contentFrameLayout.visibility = View.GONE
            statsLayout.visibility = View.GONE
            Log.d(TAG, "updateOverlayContent: navigationBarLayout GONE, contentFrameLayout GONE, statsLayout GONE (collapsed).")
        }

        // Update the window layout parameters to reflect the size change
        overlayLayoutParams?.let { params ->
            if (overlayRootView.parent != null) {
                try {
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                    // Adjust flags based on whether terminal is active and overlay is expanded
                    if (isOverlayExpanded && currentOverlaySection == OverlaySection.TERMINAL) {
                        params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
                        params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv())
                    } else {
                        params.flags = (params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                        params.flags = (params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                    }

                    windowManager.updateViewLayout(overlayRootView, params)
                    Log.d(TAG, "Service: Overlay window layout updated. Expanded: $isOverlayExpanded, Section: $currentOverlaySection, new size: ${params.width}x${params.height}, flags: ${params.flags}")
                } catch (e: Exception) {
                    Log.e(TAG, "Service: Error updating overlay window layout: ${e.message}", e)
                }
            }
        }
        Log.d(TAG, "Service: Overlay content update finished.")
    }

    // Helper to create a rounded rectangle drawable for button backgrounds
    private fun createRoundedRectDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dpToPx(4)
        }
    }


    /**
     * Populates the featuresLayout with toggleable TextViews.
     */
    private fun updateFeatureListContent() {
        featureListUpdateJob?.cancel()
        featuresLayout.removeAllViews()
        Log.d(TAG, "updateFeatureListContent: featuresLayout cleared. Cancelling previous job.")

        featureListUpdateJob = serviceScope.launch {
            val selectedTitles = DataStoreManager.getSelectedOverlayFeaturesFlow(applicationContext).first()
            val allFeatures = FeatureRepository.getAllFeaturesFlow(applicationContext).first()
            val featuresToDisplay = allFeatures.filter { selectedTitles.contains(it.title) }
            Log.d(TAG, "updateFeatureListContent: Features to display: ${featuresToDisplay.size} (titles: $selectedTitles)")

            withContext(Dispatchers.Main) {
                if (featuresToDisplay.isEmpty()) {
                    val noFeaturesText = TextView(applicationContext).apply {
                        text = "No features selected for overlay. Go to 'Overlay Selection' in app settings."
                        textSize = 14f
                        setTextColor(secondaryTextColor)
                        setPadding(dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt(), dpToPx(8).toInt())
                    }
                    featuresLayout.addView(noFeaturesText)
                    Log.d(TAG, "updateFeatureListContent: Added 'No features selected' text.")
                } else {
                    featuresToDisplay.forEach { feature ->
                        val featureTextView = TextView(applicationContext).apply {
                            text = feature.title.replace("\n", " ", ignoreCase = true)
                            textSize = 14f
                            setPadding(dpToPx(8).toInt(), dpToPx(4).toInt(), dpToPx(8).toInt(), dpToPx(4).toInt())
                            val isActive = currentFeatureStates[feature.title] == true
                            setTextColor(if (isActive) accentColor else primaryTextColor)
                        }

                        val featureSwitch = Switch(applicationContext).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            isChecked = currentFeatureStates[feature.title] == true
                            setOnCheckedChangeListener { _, newCheckedState ->
                                serviceScope.launch {
                                    DataStoreManager.setFeatureEnabled(applicationContext, feature.title, newCheckedState)
                                    currentFeatureStates = currentFeatureStates.toMutableMap().apply { put(feature.title, newCheckedState) }
                                    featureTextView.setTextColor(if (newCheckedState) accentColor else primaryTextColor)

                                    if (!newCheckedState) {
                                        feature.resetCommand?.let { resetCmd ->
                                            ShellUtils.runShellCommand(resetCmd)
                                            Toast.makeText(applicationContext, "${feature.title} reset.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (!feature.canLoopAsSpecialCase) {
                                            ShellUtils.runShellCommand(feature.command)
                                            Toast.makeText(applicationContext, "${feature.title} activated.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        val featureRowLayout = LinearLayout(applicationContext).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dpToPx(4).toInt(), dpToPx(2).toInt(), dpToPx(4).toInt(), dpToPx(2).toInt())
                        }
                        featureRowLayout.addView(featureTextView)
                        featureRowLayout.addView(View(applicationContext).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                        })
                        featureRowLayout.addView(featureSwitch)
                        featuresLayout.addView(featureRowLayout)
                    }
                    Log.d(TAG, "updateFeatureListContent: Added ${featuresToDisplay.size} feature TextViews with Switches.")
                }
            }
        }
    }

    /**
     * Starts a coroutine to periodically update CPU and Memory stats.
     */
    private fun startStatsUpdates() {
        statsUpdateJob?.cancel()
        statsUpdateJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateCpuAndMemoryUsage()
                delay(1000L)
            }
        }
    }

    /**
     * Fetches and updates CPU and Memory usage.
     */
    private suspend fun updateCpuAndMemoryUsage() {
        Log.d(TAG, "updateCpuAndMemoryUsage: Attempting to fetch CPU and Memory info.")
        val cpuInfo = ShellUtils.executeCommandWithOutput("cat /proc/stat")
        val memInfo = ShellUtils.executeCommandWithOutput("cat /proc/meminfo")
        val cpuFreqInfo = ShellUtils.executeCommandWithOutput("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")


        cpuInfo?.let {
            val cpuLine = it.split(Regex("\\s+")).filter { s -> s.isNotEmpty() }
            if (cpuLine.size >= 8) {
                val user = cpuLine[1].toLong()
                val nice = cpuLine[2].toLong()
                val system = cpuLine[3].toLong()
                val idle = cpuLine[4].toLong()
                val iowait = cpuLine[5].toLong()
                val irq = cpuLine[6].toLong()
                val softirq = cpuLine[7].toLong()
                val steal = cpuLine[8].toLong()

                val currentTotal = user + nice + system + idle + iowait + irq + softirq + steal
                val currentIdle = idle + iowait

                if (lastCpuTotal != 0L) {
                    val totalDiff = currentTotal - lastCpuTotal
                    val idleDiff = currentIdle - lastCpuIdle

                    val cpuPercentage = if (totalDiff > 0) {
                        ((totalDiff - idleDiff).toFloat() / totalDiff * 100).toInt()
                    } else 0

                    withContext(Dispatchers.Main) {
                        cpuUsageTextView.text = "CPU: $cpuPercentage%"
                        Log.d(TAG, "updateCpuAndMemoryUsage: CPU updated to $cpuPercentage%")
                    }
                }
                lastCpuTotal = currentTotal
                lastCpuIdle = currentIdle
            } else {
                Log.w(TAG, "updateCpuAndMemoryUsage: /proc/stat line did not have enough parts: $cpuLine")
            }
        } ?: Log.e(TAG, "updateCpuAndMemoryUsage: Failed to get CPU info from /proc/stat. Output was null.")

        cpuFreqInfo?.let {
            val freqKHz = it.trim().toLongOrNull()
            withContext(Dispatchers.Main) {
                cpuFreqTextView.text = if (freqKHz != null) "CPU Freq: ${freqKHz / 1000} MHz" else "CPU Freq: -- MHz"
                Log.d(TAG, "updateCpuAndMemoryUsage: CPU Freq updated: ${cpuFreqTextView.text}")
            }
        } ?: Log.e(TAG, "updateCpuAndMemoryUsage: Failed to get CPU frequency from /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq. Output was null.")


        memInfo?.let {
            val memTotalLine = it.split("\n").firstOrNull { line -> line.startsWith("MemTotal:") }
            val memAvailableLine = it.split("\n").firstOrNull { line -> line.startsWith("MemAvailable:") }


            if (memTotalLine != null && memAvailableLine != null) {
                val totalMemKb = memTotalLine.replace("MemTotal:", "").trim().split(" ")[0].toLongOrNull() ?: 0
                val availableMemKb = memAvailableLine.replace("MemAvailable:", "").trim().split(" ")[0].toLongOrNull() ?: 0

                val usedMemKb = totalMemKb - availableMemKb

                withContext(Dispatchers.Main) {
                    memUsageTextView.text = "RAM: ${usedMemKb / 1024} MB / ${totalMemKb / 1024} MB"
                    Log.d(TAG, "updateCpuAndMemoryUsage: RAM updated to ${usedMemKb / 1024} MB / ${totalMemKb / 1024} MB")
                }
            } else {
                Log.w(TAG, "updateCpuAndMemoryUsage: /proc/meminfo did not contain MemTotal or MemAvailable. MemInfo: $memInfo")
            }
        } ?: Log.e(TAG, "updateCpuAndMemoryUsage: Failed to get Memory info from /proc/meminfo. Output was null.")

        withContext(Dispatchers.Main) {
            memFreqTextView.text = "Mem Freq: -- MHz"
        }
    }

    /**
     * Executes the command from the terminal input field and appends output.
     */
    private fun executeTerminalCommand() {
        val command = terminalInput.text.toString().trim()
        if (command.isBlank()) {
            Toast.makeText(this, "Command cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        terminalInput.setText("")

        serviceScope.launch(Dispatchers.IO) {
            val output = ShellUtils.executeCommandWithOutput(command)
            withContext(Dispatchers.Main) {
                terminalOutput.append("> $command\n")
                if (output != null) {
                    terminalOutput.append(output)
                } else {
                    terminalOutput.append("[ERROR or NO OUTPUT]\n")
                }
                terminalOutput.append("\n")
                terminalScrollView.post {
                    terminalScrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }
}