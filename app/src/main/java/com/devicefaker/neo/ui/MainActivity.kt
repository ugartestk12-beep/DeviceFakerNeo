package com.devicefaker.neo.ui

import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devicefaker.neo.data.DeviceProfile
import com.devicefaker.neo.data.PrefsStore
import com.devicefaker.neo.data.Presets
import com.devicefaker.neo.data.RandomGen
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    // Hooked by FakerHook to return true when active in LSPosed
    fun isModuleActive(): Boolean = false

    private var currentTargetPkg: String? = null // null = Global Profile
    private val inputs = mutableMapOf<String, EditText>()
    private lateinit var targetLabel: TextView
    private lateinit var enabledSwitch: SwitchMaterial
    private lateinit var resetAppBtn: MaterialButton
    private lateinit var autoClearSwitch: SwitchMaterial
    private lateinit var clearAppsInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(36), dp(16), dp(32))
        }
        rootScroll.addView(container)
        setContentView(rootScroll)

        // Header
        container.addView(TextView(this).apply {
            text = "⚡ DeviceFaker Neo"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })

        // Module Status Banner
        val active = isModuleActive()
        container.addView(makeCard(if (active) "#064E3B" else "#7C2D12").apply {
            val tv = TextView(this@MainActivity).apply {
                text = if (active) {
                    "🟢 LSPosed Module ĐANG HOẠT ĐỘNG\nChỉ cần tick app trong LSPosed Manager, mở app là tự động ăn Fake!"
                } else {
                    "🟠 Module CHƯA KÍCH HOẠT trong LSPosed\nHãy mở LSPosed Manager → Modules → bật DeviceFaker Neo → tick các app cần fake."
                }
                textSize = 13f
                setTextColor(Color.WHITE)
            }
            addView(tv)
        })

        // Target Selector Card (Global vs Per-App + Multi-App Enable)
        container.addView(makeCard("#111827").apply {
            val box = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }

            targetLabel = TextView(this@MainActivity).apply {
                text = "🎯 Đang chỉnh: Cấu hình Chung (Global)"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#60A5FA"))
                setPadding(0, 0, 0, dp(8))
            }
            box.addView(targetLabel)

            enabledSwitch = SwitchMaterial(this@MainActivity).apply {
                text = "Bật Fake cho cấu hình này"
                setTextColor(Color.WHITE)
                isChecked = true
            }
            box.addView(enabledSwitch)

            val btnRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
            }
            btnRow.addView(makeButton("📱 Chọn App (Riêng)", "#3B82F6") { showPickSingleAppDialog() }, weightParams())
            btnRow.addView(makeButton("☑️ Chọn Nhiều App", "#10B981") { showMultiAppEnableDialog() }, weightParams())
            box.addView(btnRow)

            val btnRow2 = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, 0)
            }
            btnRow2.addView(makeButton("🌐 Về Global", "#374151") {
                currentTargetPkg = null
                loadCurrentProfile()
            }, weightParams())

            resetAppBtn = makeButton("🗑 Xóa cấu hình riêng", "#EF4444") {
                currentTargetPkg?.let { pkg ->
                    PrefsStore.clearAppProfile(this@MainActivity, pkg)
                    Toast.makeText(this@MainActivity, "Đã xóa cấu hình riêng của $pkg", Toast.LENGTH_SHORT).show()
                    currentTargetPkg = null
                    loadCurrentProfile()
                }
            }
            resetAppBtn.visibility = View.GONE
            btnRow2.addView(resetAppBtn, weightParams())
            box.addView(btnRow2)

            addView(box)
        })

        // Preset & Random All Card
        container.addView(makeCard("#111827").apply {
            val box = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            box.addView(sectionTitle("🌟 Preset Flagship & Random Nhanh"))

            val presetNames = Presets.list.map { it.first }
            val spinner = Spinner(this@MainActivity).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, presetNames)
                setBackgroundColor(Color.parseColor("#1F2937"))
            }
            box.addView(spinner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
            ).apply { bottomMargin = dp(8) })

            val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(makeButton("📥 Nạp Preset", "#2563EB") {
                val p = Presets.list[spinner.selectedItemPosition].second.copy()
                p.randomizeIds()
                fillForm(p)
                Toast.makeText(this@MainActivity, "Đã nạp Preset + Random ID!", Toast.LENGTH_SHORT).show()
            }, weightParams())
            row.addView(makeButton("🎲 Random Toàn Bộ ID", "#7C3AED") {
                val p = readForm()
                p.randomizeIds()
                fillForm(p)
            }, weightParams())
            box.addView(row)

            addView(box)
        })

        // Hardware & Build Card
        container.addView(makeCard("#111827").apply {
            val box = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            box.addView(sectionTitle("📱 Thông Số Phần Cứng (Build & SystemProperties)"))
            addField(box, "manufacturer", "Manufacturer (Hãng)")
            addField(box, "brand", "Brand (Thương hiệu)")
            addField(box, "model", "Model (Mã máy)")
            addField(box, "device", "Device (Tên mã)")
            addField(box, "product", "Product")
            addField(box, "board", "Board")
            addField(box, "hardware", "Hardware")
            addField(box, "socModel", "SoC Model (Chip)")
            addField(box, "socManufacturer", "SoC Manufacturer")
            addField(box, "release", "Android Release (vd: 15)")
            addField(box, "sdkInt", "SDK INT (để trống = giữ nguyên an toàn)")
            addField(box, "fingerprint", "Build Fingerprint")
            addView(box)
        })

        // Unique Identifiers & Network Card
        container.addView(makeCard("#111827").apply {
            val box = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            box.addView(sectionTitle("🔑 Định Danh Thiết Bị, SIM & Mạng"))
            addField(box, "imei", "IMEI (Luhn chuẩn 15 số)") { RandomGen.imei() }
            addField(box, "androidId", "Android ID (16 hex)") { RandomGen.androidId() }
            addField(box, "serial", "Serial Number") { RandomGen.serial() }
            addField(box, "gsfId", "GSF ID (Google Services Framework)") { RandomGen.gsfId() }
            addField(box, "gaid", "Google Advertising ID (GAID)") { RandomGen.gaid() }
            addField(box, "wifiMac", "Wi-Fi MAC Address") { RandomGen.mac() }
            addField(box, "btMac", "Bluetooth MAC") { RandomGen.mac().uppercase() }
            addField(box, "wifiSsid", "Wi-Fi SSID") { RandomGen.ssid() }
            addField(box, "wifiBssid", "Wi-Fi BSSID") { RandomGen.mac() }
            addField(box, "imsi", "IMSI") { RandomGen.imsi(inputs["simOperator"]?.text?.toString().orEmpty().ifBlank { "45204" }) }
            addField(box, "iccid", "ICCID (Seri SIM)") { RandomGen.iccid() }
            addField(box, "phone", "Số điện thoại") { RandomGen.phone() }
            addField(box, "simOperator", "SIM Operator (45204 = Viettel)")
            addField(box, "simCountry", "SIM Country ISO (vn)")
            addField(box, "operatorName", "Tên nhà mạng (Viettel)")
            addView(box)
        })

        // Auto Clear Data (Root) Card
        container.addView(makeCard("#111827").apply {
            val box = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            box.addView(sectionTitle("🧹 Tự Động Xóa Data App (Cần quyền Root)"))

            autoClearSwitch = SwitchMaterial(this@MainActivity).apply {
                text = "Tự động xóa Data các app đã chọn khi bấm Lưu"
                setTextColor(Color.WHITE)
                isChecked = PrefsStore.isAutoClearEnabled(this@MainActivity)
                setOnCheckedChangeListener { _, isChecked ->
                    PrefsStore.setAutoClearEnabled(this@MainActivity, isChecked)
                }
            }
            box.addView(autoClearSwitch)

            clearAppsInfo = TextView(this@MainActivity).apply {
                textSize = 12f
                setTextColor(Color.parseColor("#9CA3AF"))
                setPadding(0, dp(2), 0, dp(8))
            }
            box.addView(clearAppsInfo)
            updateClearAppsInfo()

            val clearRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            clearRow.addView(makeButton("📋 Chọn App xóa Data", "#D97706") {
                showSelectClearAppsDialog()
            }, weightParams())
            clearRow.addView(makeButton("🧹 Xóa Data ngay", "#DC2626") {
                clearSelectedAppsData()
            }, weightParams())
            box.addView(clearRow)

            addView(box)
        })

        // Save Button
        container.addView(makeButton("💾 LƯU CẤU HÌNH (Áp dụng ngay cho LSPosed)", "#10B981") {
            val profile = readForm()
            val pkg = currentTargetPkg
            if (pkg == null) {
                PrefsStore.saveGlobalProfile(this, profile)
            } else {
                PrefsStore.saveAppProfile(this, pkg, profile)
                PrefsStore.setAppEnabled(this, pkg, true)
            }
            if (PrefsStore.isAutoClearEnabled(this)) {
                clearSelectedAppsData(afterSave = true)
            } else {
                val msg = if (pkg == null) {
                    "✅ Đã lưu Cấu hình Chung (Global)! Hãy tắt và mở lại app mục tiêu."
                } else {
                    "✅ Đã lưu cấu hình riêng cho $pkg!"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
            ).apply { topMargin = dp(8) }
        })

        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        val pkg = currentTargetPkg
        val profile = if (pkg == null) {
            targetLabel.text = "🎯 Đang chỉnh: Cấu hình Chung (Global)"
            resetAppBtn.visibility = View.GONE
            PrefsStore.getGlobalProfile(this)
        } else {
            targetLabel.text = "🎯 Cấu hình riêng cho: $pkg"
            resetAppBtn.visibility = View.VISIBLE
            PrefsStore.getAppProfile(this, pkg) ?: PrefsStore.getGlobalProfile(this).copy()
        }
        fillForm(profile)
    }

    private fun fillForm(p: DeviceProfile) {
        enabledSwitch.isChecked = p.enabled
        inputs["manufacturer"]?.setText(p.manufacturer)
        inputs["brand"]?.setText(p.brand)
        inputs["model"]?.setText(p.model)
        inputs["device"]?.setText(p.device)
        inputs["product"]?.setText(p.product)
        inputs["board"]?.setText(p.board)
        inputs["hardware"]?.setText(p.hardware)
        inputs["socModel"]?.setText(p.socModel)
        inputs["socManufacturer"]?.setText(p.socManufacturer)
        inputs["release"]?.setText(p.release)
        inputs["sdkInt"]?.setText(p.sdkInt)
        inputs["fingerprint"]?.setText(p.fingerprint)
        inputs["imei"]?.setText(p.imei)
        inputs["androidId"]?.setText(p.androidId)
        inputs["serial"]?.setText(p.serial)
        inputs["gsfId"]?.setText(p.gsfId)
        inputs["gaid"]?.setText(p.gaid)
        inputs["wifiMac"]?.setText(p.wifiMac)
        inputs["btMac"]?.setText(p.btMac)
        inputs["wifiSsid"]?.setText(p.wifiSsid)
        inputs["wifiBssid"]?.setText(p.wifiBssid)
        inputs["imsi"]?.setText(p.imsi)
        inputs["iccid"]?.setText(p.iccid)
        inputs["phone"]?.setText(p.phone)
        inputs["simOperator"]?.setText(p.simOperator)
        inputs["simCountry"]?.setText(p.simCountry)
        inputs["operatorName"]?.setText(p.operatorName)
    }

    private fun readForm(): DeviceProfile {
        fun g(k: String) = inputs[k]?.text?.toString()?.trim().orEmpty()
        return DeviceProfile(
            enabled = enabledSwitch.isChecked,
            manufacturer = g("manufacturer"),
            brand = g("brand"),
            model = g("model"),
            device = g("device"),
            product = g("product"),
            board = g("board"),
            hardware = g("hardware"),
            socModel = g("socModel"),
            socManufacturer = g("socManufacturer"),
            release = g("release"),
            sdkInt = g("sdkInt"),
            fingerprint = g("fingerprint"),
            imei = g("imei"),
            androidId = g("androidId"),
            serial = g("serial"),
            gsfId = g("gsfId"),
            gaid = g("gaid"),
            wifiMac = g("wifiMac"),
            btMac = g("btMac"),
            wifiSsid = g("wifiSsid"),
            wifiBssid = g("wifiBssid"),
            imsi = g("imsi"),
            iccid = g("iccid"),
            phone = g("phone"),
            simOperator = g("simOperator"),
            simCountry = g("simCountry"),
            operatorName = g("operatorName")
        )
    }

    private fun getInstalledAppsList(): List<Pair<String, String>> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { it.packageName != packageName }
            .filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    it.packageName in setOf(
                        "com.android.vending",
                        "com.google.android.apps.subscriptions.red",
                        "com.google.android.gm",
                        "com.android.chrome"
                    )
            }
            .map { pm.getApplicationLabel(it).toString() to it.packageName }
            .sortedBy { it.first.lowercase() }
        return apps
    }

    private fun showPickSingleAppDialog() {
        val apps = getInstalledAppsList()
        val labels = apps.map { (name, pkg) ->
            val hasCustom = PrefsStore.getAppProfile(this, pkg) != null
            (if (hasCustom) "⭐ " else "") + "$name\n($pkg)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Chọn app để tạo cấu hình riêng")
            .setItems(labels) { _, which ->
                currentTargetPkg = apps[which].second
                loadCurrentProfile()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showMultiAppEnableDialog() {
        val apps = getInstalledAppsList()
        val enabled = PrefsStore.getEnabledApps(this)
        val labels = apps.map { "${it.first} (${it.second})" }.toTypedArray()
        val checked = BooleanArray(apps.size) { i -> enabled.contains(apps[i].second) }

        AlertDialog.Builder(this)
            .setTitle("Chọn nhiều app áp dụng Fake")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Lưu danh sách") { _, _ ->
                val newSet = mutableSetOf<String>()
                for (i in apps.indices) {
                    if (checked[i]) newSet.add(apps[i].second)
                }
                PrefsStore.getPrefs(this).edit().putStringSet(PrefsStore.KEY_ENABLED_APPS, newSet).apply()
                Toast.makeText(this, "Đã chọn ${newSet.size} ứng dụng! (Nhớ tick các app này trong LSPosed Manager)", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Áp dụng tất cả (Theo Scope LSPosed)") { _, _ ->
                PrefsStore.getPrefs(this).edit().putStringSet(PrefsStore.KEY_ENABLED_APPS, emptySet()).apply()
                Toast.makeText(this, "Đang áp dụng cho mọi app được tick trong LSPosed!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateClearAppsInfo() {
        val list = PrefsStore.getClearDataApps(this)
        clearAppsInfo.text = if (list.isEmpty()) {
            "Chưa chọn app nào để tự động xóa Data."
        } else {
            "Đã chọn ${list.size} app: ${list.joinToString(", ")}"
        }
    }

    private fun showSelectClearAppsDialog() {
        val apps = getInstalledAppsList()
        val selected = PrefsStore.getClearDataApps(this)
        val labels = apps.map { "${it.first} (${it.second})" }.toTypedArray()
        val checked = BooleanArray(apps.size) { i -> selected.contains(apps[i].second) }

        AlertDialog.Builder(this)
            .setTitle("Chọn riêng các app cần xóa Data")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Lưu danh sách") { _, _ ->
                val newSet = mutableSetOf<String>()
                for (i in apps.indices) {
                    if (checked[i]) newSet.add(apps[i].second)
                }
                PrefsStore.setClearDataApps(this, newSet)
                updateClearAppsInfo()
                Toast.makeText(this, "Đã lưu ${newSet.size} app cần xóa Data!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Play Store + Google One") { _, _ ->
                val quick = setOf("com.android.vending", "com.google.android.apps.subscriptions.red")
                PrefsStore.setClearDataApps(this, quick)
                updateClearAppsInfo()
                Toast.makeText(this, "Đã chọn nhanh Play Store + Google One!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun clearSelectedAppsData(afterSave: Boolean = false) {
        val blocked = setOf("android", "system", "com.android.systemui", packageName)
        val targets = PrefsStore.getClearDataApps(this).filter { it !in blocked }
        if (targets.isEmpty()) {
            val msg = if (afterSave) {
                "✅ Đã lưu cấu hình! (Chưa chọn app nào trong danh sách xóa Data)"
            } else {
                "⚠️ Bạn chưa chọn app nào trong danh sách xóa Data!"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "⏳ Đang xóa Data của ${targets.size} app qua Root...", Toast.LENGTH_SHORT).show()
        Thread {
            var okCount = 0
            var rootDenied = false
            for (pkg in targets) {
                try {
                    val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm clear $pkg"))
                    val code = proc.waitFor()
                    if (code == 0) okCount++ else if (code == 13 || code == 126 || code == 127) rootDenied = true
                } catch (_: Exception) {
                    rootDenied = true
                }
            }
            runOnUiThread {
                val prefix = if (afterSave) "✅ Đã lưu cấu hình & " else "✅ "
                val msg = when {
                    okCount > 0 -> "${prefix}xóa Data thành công $okCount/${targets.size} app!"
                    rootDenied -> "⚠️ Không có quyền Root! Hãy mở ReSukiSU → cấp quyền Root cho DeviceFaker Neo."
                    else -> "⚠️ Không xóa được Data (kiểm tra quyền Root trong ReSukiSU)."
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun addField(parent: LinearLayout, key: String, labelText: String, randomizer: (() -> String)? = null) {
        parent.addView(TextView(this).apply {
            text = labelText
            textSize = 11.5f
            setTextColor(Color.parseColor("#9CA3AF"))
            setPadding(0, dp(6), 0, dp(2))
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val et = EditText(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1F2937"))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        inputs[key] = et
        row.addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        if (randomizer != null) {
            val btn = MaterialButton(this).apply {
                text = "🎲"
                setBackgroundColor(Color.parseColor("#374151"))
                setTextColor(Color.WHITE)
                setPadding(dp(8), 0, dp(8), 0)
                minWidth = dp(44)
                setOnClickListener { et.setText(randomizer()) }
            }
            row.addView(btn, LinearLayout.LayoutParams(dp(48), dp(42)).apply { leftMargin = dp(6) })
        }
        parent.addView(row)
    }

    private fun makeCard(bgHex: String): MaterialCardView {
        return MaterialCardView(this).apply {
            setCardBackgroundColor(Color.parseColor(bgHex))
            radius = dp(12).toFloat()
            setContentPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
    }

    private fun sectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(6))
        }
    }

    private fun makeButton(text: String, bgHex: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            this.text = text
            textSize = 12.5f
            isAllCaps = false
            setBackgroundColor(Color.parseColor(bgHex))
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
        }
    }

    private fun weightParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(6)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
