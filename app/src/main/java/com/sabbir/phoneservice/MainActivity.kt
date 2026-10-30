package com.sabbir.phoneservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var wifiStatus: TextView
    private lateinit var wifiResults: TextView
    private lateinit var wifiManager: WifiManager

    private val devices = linkedMapOf<String, String>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            try {
                val name = if (Build.VERSION.SDK_INT >= 31 &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    "নাম প্রকাশ করেনি"
                } else {
                    result.device.name ?: "নাম প্রকাশ করেনি"
                }

                devices[result.device.address] =
                    "$name\nসিগন্যাল: ${result.rssi} dBm"

                runOnUiThread { showBluetoothResults() }
            } catch (_: Exception) {}
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                if (::status.isInitialized) {
                    status.text = "স্ক্যান ব্যর্থ: $errorCode"
                }
            }
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showWifiResults()
        }
    }

    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(wifiReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiReceiver, filter)
        }
        receiverRegistered = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 16, 32))
            setPadding(20, 25, 20, 15)
        }

        val header = TextView(this).apply {
            text = "SABBIR PHONE SERVICE"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "SMART DEVICE DIAGNOSTICS"
            textSize = 12f
            setTextColor(Color.rgb(80, 190, 255))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(content)

        root.addView(header)
        root.addView(subtitle)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        showDashboard()
    }

    private fun addCard(title: String, description: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.rgb(24, 38, 62))
        }

        val heading = TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val detail = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 12)
        }

        val button = Button(this).apply {
            text = "OPEN"
            setOnClickListener { action() }
        }

        card.addView(heading)
        card.addView(detail)
        card.addView(button)

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 8, 0, 12)
        content.addView(card, params)
    }

    private fun showDashboard() {
        content.removeAllViews()

        addCard("📶 Bluetooth Scanner",
            "BLE ডিভাইসের নাম ও সিগন্যাল দেখুন") {
            showBluetooth()
        }

        addCard("📡 Wi-Fi Scanner",
            "আশপাশের Wi-Fi নেটওয়ার্ক খুঁজুন") {
            showWifi()
        }

        addCard("📱 Mobile Signal",
            "নিজের ফোনের মোবাইল নেটওয়ার্ক তথ্য") {
            showSignal()
        }

        addCard("🛠 Phone Service",
            "ফোনের সাধারণ সমস্যা সমাধানের নির্দেশিকা") {
            showService()
        }
    }

    private fun showBluetooth() {
        content.removeAllViews()

        addCard("Bluetooth Scanner", "BLE ডিভাইস স্ক্যান করুন") {
            checkBluetoothPermission()
        }

        status = TextView(this).apply {
            text = "স্ক্যানের জন্য প্রস্তুত"
            textSize = 15f
            setTextColor(Color.GREEN)
            setPadding(8, 20, 8, 20)
        }
        content.addView(status)

        addCard("STOP SCAN", "Bluetooth স্ক্যান বন্ধ করুন") {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                status.text = "স্ক্যান বন্ধ হয়েছে"
            } catch (_: Exception) {
                status.text = "স্ক্যান বন্ধ করা যায়নি"
            }
        }

        addBackButton()
    }

    private fun checkBluetoothPermission() {
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
        } else {
            startBluetoothScan()
        }
    }

    private fun startBluetoothScan() {
        try {
            if (bluetoothAdapter?.isEnabled != true) {
                status.text = "আগে Bluetooth চালু করুন"
                return
            }

            devices.clear()
            showBluetoothResults()

            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            status.text = "BLE ডিভাইস খোঁজা হচ্ছে..."
        } catch (_: Exception) {
            status.text = "Bluetooth স্ক্যান শুরু করা যায়নি"
        }
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
            startBluetoothScan()
        }

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startWifiScan()
        }
    }

    private fun showBluetoothResults() {
        if (!::status.isInitialized) return

        status.text = if (devices.isEmpty()) {
            "এখনো কোনো BLE ডিভাইস পাওয়া যায়নি"
        } else {
            devices.values.joinToString("\n\n")
        }
    }

    private fun showWifi() {
        content.removeAllViews()

        addCard("📡 Wi-Fi Scanner",
            "Wi-Fi চালু রেখে স্ক্যান করুন") {
            checkWifiPermission()
        }

        wifiStatus = TextView(this).apply {
            text = "স্ক্যানের জন্য প্রস্তুত"
            textSize = 15f
            setTextColor(Color.GREEN)
            setPadding(8, 18, 8, 18)
        }
        content.addView(wifiStatus)

        wifiResults = TextView(this).apply {
            text = "এখনো কোনো নেটওয়ার্ক পাওয়া যায়নি"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(12, 15, 12, 15)
        }
        content.addView(wifiResults)

        addCard("🔄 SCAN AGAIN", "আবার Wi-Fi স্ক্যান করুন") {
            checkWifiPermission()
        }

        addBackButton()
    }

    private fun checkWifiPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        } else {
            startWifiScan()
        }
    }

    private fun startWifiScan() {
        try {
            if (!wifiManager.isWifiEnabled) {
                wifiStatus.text = "আগে ফোনের Wi-Fi চালু করুন"
                return
            }

            wifiStatus.text = "Wi-Fi নেটওয়ার্ক খোঁজা হচ্ছে..."

            val started = wifiManager.startScan()

            if (!started) {
                wifiStatus.text =
                    "স্ক্যান সীমাবদ্ধ হতে পারে। আগের ফলাফল দেখানো হচ্ছে।"
                showWifiResults()
            }
        } catch (_: SecurityException) {
            wifiStatus.text = "Location অনুমতি প্রয়োজন"
        } catch (_: Exception) {
            wifiStatus.text = "Wi-Fi স্ক্যান করা যায়নি"
        }
    }

    private fun showWifiResults() {
        try {
            val results = wifiManager.scanResults

            if (results.isEmpty()) {
                wifiStatus.text =
                    "নেটওয়ার্ক পাওয়া যায়নি। Location চালু করে আবার চেষ্টা করুন।"
                wifiResults.text = "কোনো Wi-Fi ফলাফল নেই"
                return
            }

            wifiStatus.text = "মোট ${results.size}টি নেটওয়ার্ক পাওয়া গেছে"

            wifiResults.text = results
                .sortedByDescending { it.level }
                .joinToString("\n\n") { network ->

                    val name = network.SSID
                        .takeIf { it.isNotBlank() && it != "<unknown ssid>" }
                        ?: "নাম প্রকাশিত নয়"

                    """
                    📶 $name
                    Signal: ${network.level} dBm
                    Frequency: ${network.frequency} MHz
                    Security: ${network.capabilities}
                    """.trimIndent()
                }

        } catch (_: SecurityException) {
            wifiStatus.text = "Wi-Fi অনুমতি পাওয়া যায়নি"
        } catch (_: Exception) {
            wifiStatus.text = "ফলাফল পড়া যায়নি"
        }
    }

    private fun showSignal() {
        content.removeAllViews()
        addCard("Mobile Signal",
            "পরের ধাপে মোবাইল নেটওয়ার্ক তথ্য যুক্ত হবে।") {}
        addBackButton()
    }

    private fun showService() {
        content.removeAllViews()
        addCard("Phone Service",
            "ফোনের সাধারণ সমস্যা সমাধানের গাইড পরের ধাপে যুক্ত হবে।") {}
        addBackButton()
    }

    private fun addBackButton() {
        addCard("⬅ BACK", "মূল মেনুতে ফিরে যান") {
            showDashboard()
        }
    }

    override fun onDestroy() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}

        if (receiverRegistered) {
            unregisterReceiver(wifiReceiver)
            receiverRegistered = false
        }

        super.onDestroy()
    }
}
