
package com.sabbir.phoneservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var adapter: BluetoothAdapter? = null
    private var scannerActive = false
    private val devices = linkedMapOf<String, String>()
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            try {
                val device = result.device
                val name = if (Build.VERSION.SDK_INT >= 31) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                        device.name ?: "নাম প্রকাশ করেনি"
                    else "নাম প্রকাশ করেনি"
                } else device.name ?: "নাম প্রকাশ করেনি"

                val address = device.address
                devices[address] = "$name\nসংকেত: ${result.rssi} dBm"
                runOnUiThread { showDevices() }
            } catch (_: SecurityException) {}
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                status.text = "স্ক্যান ব্যর্থ: $errorCode"
                scannerActive = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 20)
            setBackgroundColor(Color.rgb(12, 20, 38))
        }

        val title = TextView(this).apply {
            text = "SABBIR PHONE SERVICE"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Bluetooth স্ক্যানার প্রস্তুত"
            textSize = 15f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 25)
        }

        val scanButton = Button(this).apply {
            text = "SCAN DEVICES"
            setOnClickListener { checkPermissionsAndScan() }
        }

        val stopButton = Button(this).apply {
            text = "STOP SCAN"
            setOnClickListener { stopScan() }
        }

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(title)
        root.addView(status)
        root.addView(scanButton)
        root.addView(stopButton)

        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
    }

    private fun checkPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 100)
        } else startScan()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startScan()
        } else {
            status.text = "Bluetooth অনুমতি প্রয়োজন"
        }
    }

    private fun startScan() {
        try {
            val bluetooth = adapter
            if (bluetooth == null || !bluetooth.isEnabled) {
                status.text = "আগে Bluetooth চালু করুন"
                return
            }

            devices.clear()
            showDevices()

            val scanner = bluetooth.bluetoothLeScanner
            if (scanner == null) {
                status.text = "BLE Scanner পাওয়া যায়নি"
                return
            }

            scanner.startScan(callback)
            scannerActive = true
            status.text = "আশপাশের BLE ডিভাইস খোঁজা হচ্ছে..."

            handler.postDelayed({
                stopScan()
            }, 15000)

        } catch (_: SecurityException) {
            status.text = "Bluetooth অনুমতি নেই"
        }
    }

    private fun stopScan() {
        try {
            if (scannerActive) {
                adapter?.bluetoothLeScanner?.stopScan(callback)
                scannerActive = false
                status.text = "স্ক্যান শেষ"
            }
        } catch (_: SecurityException) {}
    }

    private fun showDevices() {
        list.removeAllViews()

        if (devices.isEmpty()) {
            val empty = TextView(this).apply {
                text = "এখনো কোনো ডিভাইস পাওয়া যায়নি"
                setTextColor(Color.LTGRAY)
                textSize = 15f
                setPadding(10, 30, 10, 30)
            }
            list.addView(empty)
            return
        }

        devices.values.forEach { info ->
            val item = TextView(this).apply {
                text = "📱 $info"
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(15, 18, 15, 18)
                setBackgroundColor(Color.rgb(28, 42, 65))
            }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            list.addView(item, params)
        }
    }

    override fun onDestroy() {
        stopScan()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
