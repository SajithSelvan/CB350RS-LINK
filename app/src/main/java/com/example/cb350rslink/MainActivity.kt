package com.example.cb350rslink

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    private lateinit var audio: AudioManager
    private lateinit var status: TextView
    private lateinit var scanResults: TextView
    private var scannerCallback: ScanCallback? = null
    private val requestScanPermission = 401

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        buildScreen()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 24)
            setBackgroundColor(Color.rgb(13, 17, 23))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24)
        }

        val title = TextView(this).apply {
            text = "CB350RS LINK"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "2026 Honda CB350RS DLX Pro • Companion prototype"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 22)
        }
        content.addView(subtitle)

        section(content, "MUSIC CONTROL", "Sends standard Android media-key commands to the active media app.")
        val row1 = horizontalRow()
        row1.addView(actionButton("⏮ Previous") { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }, weighted())
        row1.addView(actionButton("▶ / Ⅱ Play / Pause") { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }, weighted())
        content.addView(row1)

        val row2 = horizontalRow()
        row2.addView(actionButton("Next ⏭") { sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }, weighted())
        row2.addView(actionButton("Volume −") { changeVolume(false) }, weighted())
        row2.addView(actionButton("Volume +") { changeVolume(true) }, weighted())
        content.addView(row2)

        status = TextView(this).apply {
            text = "Ready. Start music in Spotify, YouTube Music, VLC or another media app, then test these buttons."
            textSize = 13f
            setTextColor(Color.rgb(180, 205, 190))
            setPadding(0, 14, 0, 24)
        }
        content.addView(status)

        section(content, "BIKE BLUETOOTH CHECK", "This first build only scans nearby BLE devices. It does not yet pair with or send commands to the motorcycle.")
        content.addView(actionButton("Scan for nearby Bluetooth devices") { startBleScan() })
        scanResults = TextView(this).apply {
            text = "No scan performed yet."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 14, 0, 18)
        }
        content.addView(scanResults)

        val note = TextView(this).apply {
            text = "Safety: configure and test while parked. Do not look at or operate the phone while riding. Handlebar integration is not implemented in this prototype."
            textSize = 12f
            setTextColor(Color.rgb(255, 190, 120))
            setPadding(0, 18, 0, 0)
        }
        content.addView(note)

        scroll.addView(content)
        page.addView(scroll)
        setContentView(page)
    }

    private fun section(parent: LinearLayout, heading: String, description: String) {
        val h = TextView(this).apply {
            text = heading
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(235, 170, 75))
            setPadding(0, 8, 0, 4)
        }
        val d = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 12)
        }
        parent.addView(h)
        parent.addView(d)
    }

    private fun horizontalRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private fun weighted() = LinearLayout.LayoutParams(0, 54, 1f).apply {
        setMargins(3, 3, 3, 3)
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            setOnClickListener { action() }
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val now = android.os.SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        status.text = "Sent media command: ${mediaName(keyCode)}. Check the active music app."
    }

    private fun mediaName(code: Int): String = when (code) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "Play / Pause"
        KeyEvent.KEYCODE_MEDIA_NEXT -> "Next track"
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "Previous track"
        else -> "Media key"
    }

    private fun changeVolume(up: Boolean) {
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        status.text = if (up) "Media volume increased." else "Media volume decreased."
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), requestScanPermission)
            return
        }

        try {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter: BluetoothAdapter? = manager.adapter
            if (adapter == null || !adapter.isEnabled) {
                scanResults.text = "Bluetooth is off or unavailable. Turn Bluetooth on and try again."
                return
            }
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                scanResults.text = "BLE scanner unavailable on this phone."
                return
            }

            val found = linkedMapOf<String, String>()
            scanResults.text = "Scanning for 10 seconds… Keep the bike switched on and nearby."
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val name = try { device.name ?: "Unnamed BLE device" } catch (_: SecurityException) { "Protected device" }
                    val address = device.address ?: "Address unavailable"
                    found[address] = "$name  •  $address  •  RSSI ${result.rssi} dBm"
                    runOnUiThread { scanResults.text = found.values.joinToString("\n\n") }
                }

                override fun onScanFailed(errorCode: Int) {
                    runOnUiThread { scanResults.text = "BLE scan failed (code $errorCode)." }
                }
            }
            scannerCallback = callback
            scanner.startScan(callback)
            android.os.Handler(mainLooper).postDelayed({
                try { scanner.stopScan(callback) } catch (_: SecurityException) {}
                if (found.isEmpty()) scanResults.text = "No BLE devices found. Check Bluetooth and try again near the bike."
                scannerCallback = null
            }, 10_000)
        } catch (e: SecurityException) {
            scanResults.text = "Bluetooth permission was not granted."
        } catch (e: Exception) {
            scanResults.text = "Bluetooth scan error: ${e.message ?: "unknown error"}"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestScanPermission && grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBleScan()
        } else if (requestCode == requestScanPermission) {
            scanResults.text = "Bluetooth scan permission denied."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Any active scan is stopped by its 10-second completion callback.
    }
}
