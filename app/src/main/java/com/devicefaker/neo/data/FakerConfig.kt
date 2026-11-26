package com.devicefaker.neo.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

data class DeviceProfile(
    var enabled: Boolean = true,
    var manufacturer: String = "Xiaomi",
    var brand: String = "Xiaomi",
    var model: String = "24129PN74G",
    var device: String = "dada",
    var product: String = "dada_global",
    var board: String = "taro",
    var hardware: String = "qcom",
    var socModel: String = "SM8750",
    var socManufacturer: String = "Qualcomm",
    var release: String = "15",
    var sdkInt: String = "",
    var fingerprint: String = "Xiaomi/dada_global/dada:15/AQ3A.240810.002/V816.0.4.0.WOAMIXM:user/release-keys",
    var imei: String = "",
    var androidId: String = "",
    var serial: String = "",
    var gsfId: String = "",
    var gaid: String = "",
    var wifiMac: String = "",
    var btMac: String = "",
    var wifiSsid: String = "",
    var wifiBssid: String = "",
    var imsi: String = "",
    var iccid: String = "",
    var phone: String = "",
    var simOperator: String = "45204",
    var simCountry: String = "vn",
    var operatorName: String = "Viettel"
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("enabled", enabled)
        o.put("manufacturer", manufacturer)
        o.put("brand", brand)
        o.put("model", model)
        o.put("device", device)
        o.put("product", product)
        o.put("board", board)
        o.put("hardware", hardware)
        o.put("socModel", socModel)
        o.put("socManufacturer", socManufacturer)
        o.put("release", release)
        o.put("sdkInt", sdkInt)
        o.put("fingerprint", fingerprint)
        o.put("imei", imei)
        o.put("androidId", androidId)
        o.put("serial", serial)
        o.put("gsfId", gsfId)
        o.put("gaid", gaid)
        o.put("wifiMac", wifiMac)
        o.put("btMac", btMac)
        o.put("wifiSsid", wifiSsid)
        o.put("wifiBssid", wifiBssid)
        o.put("imsi", imsi)
        o.put("iccid", iccid)
        o.put("phone", phone)
        o.put("simOperator", simOperator)
        o.put("simCountry", simCountry)
        o.put("operatorName", operatorName)
        return o.toString()
    }

    companion object {
        fun fromJson(json: String?): DeviceProfile {
            if (json.isNullOrBlank()) return DeviceProfile().apply { randomizeIds() }
            return try {
                val o = JSONObject(json)
                DeviceProfile(
                    enabled = o.optBoolean("enabled", true),
                    manufacturer = o.optString("manufacturer", ""),
                    brand = o.optString("brand", ""),
                    model = o.optString("model", ""),
                    device = o.optString("device", ""),
                    product = o.optString("product", ""),
                    board = o.optString("board", ""),
                    hardware = o.optString("hardware", ""),
                    socModel = o.optString("socModel", ""),
                    socManufacturer = o.optString("socManufacturer", ""),
                    release = o.optString("release", ""),
                    sdkInt = o.optString("sdkInt", ""),
                    fingerprint = o.optString("fingerprint", ""),
                    imei = o.optString("imei", ""),
                    androidId = o.optString("androidId", ""),
                    serial = o.optString("serial", ""),
                    gsfId = o.optString("gsfId", ""),
                    gaid = o.optString("gaid", ""),
                    wifiMac = o.optString("wifiMac", ""),
                    btMac = o.optString("btMac", ""),
                    wifiSsid = o.optString("wifiSsid", ""),
                    wifiBssid = o.optString("wifiBssid", ""),
                    imsi = o.optString("imsi", ""),
                    iccid = o.optString("iccid", ""),
                    phone = o.optString("phone", ""),
                    simOperator = o.optString("simOperator", ""),
                    simCountry = o.optString("simCountry", ""),
                    operatorName = o.optString("operatorName", "")
                )
            } catch (_: Exception) {
                DeviceProfile().apply { randomizeIds() }
            }
        }
    }

    fun randomizeIds() {
        imei = RandomGen.imei()
        androidId = RandomGen.androidId()
        serial = RandomGen.serial()
        gsfId = RandomGen.gsfId()
        gaid = RandomGen.gaid()
        wifiMac = RandomGen.mac()
        btMac = RandomGen.mac().uppercase()
        wifiSsid = RandomGen.ssid()
        wifiBssid = RandomGen.mac()
        imsi = RandomGen.imsi(simOperator.ifBlank { "45204" })
        iccid = RandomGen.iccid()
        phone = RandomGen.phone()
    }
}

object RandomGen {
    private val hexChars = "0123456789abcdef"
    private val alphaNum = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun imei(): String {
        val partial = "86" + (1..12).map { Random.nextInt(0, 10) }.joinToString("")
        var sum = 0
        for (i in partial.indices) {
            var d = partial[i] - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        val check = (10 - (sum % 10)) % 10
        return partial + check
    }

    fun androidId(): String = (1..16).map { hexChars.random() }.joinToString("")
    fun serial(): String = (1..12).map { alphaNum.random() }.joinToString("")
    fun gsfId(): String = Random.nextLong(3000000000000000000L, 4999999999999999999L).toString()
    fun gaid(): String = UUID.randomUUID().toString()

    fun mac(): String {
        val first = (Random.nextInt(0, 256) and 0xFE) or 0x02
        val rest = (1..5).map { Random.nextInt(0, 256) }
        return (listOf(first) + rest).joinToString(":") { "%02x".format(it) }
    }

    fun imsi(op: String = "45204"): String =
        op.take(5).padEnd(5, '0') + (1..10).map { Random.nextInt(0, 10) }.joinToString("")

    fun iccid(): String = "898404" + (1..13).map { Random.nextInt(0, 10) }.joinToString("")

    fun phone(): String {
        val prefixes = listOf("32", "33", "34", "35", "36", "37", "38", "39", "86", "96", "97", "98")
        return "+84" + prefixes.random() + (1..7).map { Random.nextInt(0, 10) }.joinToString("")
    }

    fun ssid(): String {
        val prefixes = listOf("VNPT_", "FPT_5G_", "Viettel_", "TP-Link_")
        return prefixes.random() + (1..4).map { alphaNum.random() }.joinToString("")
    }
}

object Presets {
    val list: List<Pair<String, DeviceProfile>> = listOf(
        "Xiaomi 15 Global (24129PN74G)" to DeviceProfile(
            manufacturer = "Xiaomi", brand = "Xiaomi", model = "24129PN74G",
            device = "dada", product = "dada_global", board = "taro", hardware = "qcom",
            socModel = "SM8750", socManufacturer = "Qualcomm", release = "15", sdkInt = "",
            fingerprint = "Xiaomi/dada_global/dada:15/AQ3A.240810.002/V816.0.4.0.WOAMIXM:user/release-keys"
        ),
        "Xiaomi 15 Ultra (25010PN30G)" to DeviceProfile(
            manufacturer = "Xiaomi", brand = "Xiaomi", model = "25010PN30G",
            device = "xuanyuan", product = "xuanyuan_global", board = "taro", hardware = "qcom",
            socModel = "SM8750", socManufacturer = "Qualcomm", release = "15", sdkInt = "",
            fingerprint = "Xiaomi/xuanyuan_global/xuanyuan:15/AQ3A.240810.002/OS2.0.1.0.VOAMIXM:user/release-keys"
        ),
        "Google Pixel 9 Pro XL" to DeviceProfile(
            manufacturer = "Google", brand = "google", model = "Pixel 9 Pro XL",
            device = "komodo", product = "komodo", board = "komodo", hardware = "komodo",
            socModel = "Tensor G4", socManufacturer = "Google", release = "15", sdkInt = "",
            fingerprint = "google/komodo/komodo:15/AP4A.241205.013/12626578:user/release-keys"
        ),
        "Samsung Galaxy S25 Ultra (SM-S938B)" to DeviceProfile(
            manufacturer = "samsung", brand = "samsung", model = "SM-S938B",
            device = "pa3q", product = "pa3qxxx", board = "sun", hardware = "qcom",
            socModel = "SM8750", socManufacturer = "Qualcomm", release = "15", sdkInt = "",
            fingerprint = "samsung/pa3qxxx/pa3q:15/AP3A.240905.015.A2/S938BXXU1AYB3:user/release-keys"
        )
    )
}

object PrefsStore {
    const val PREFS_NAME = "faker_prefs"
    const val KEY_GLOBAL = "global_profile"
    const val KEY_PREFIX_APP = "app_profile_"
    const val KEY_ENABLED_APPS = "enabled_apps"
    const val KEY_AUTO_CLEAR = "auto_clear_on_save"
    const val KEY_CLEAR_APPS = "clear_data_apps"

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    fun getPrefs(context: Context): SharedPreferences {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (_: SecurityException) {
            // Fallback when LSPosed module is not activated yet
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getGlobalProfile(context: Context): DeviceProfile {
        val raw = getPrefs(context).getString(KEY_GLOBAL, null)
        return DeviceProfile.fromJson(raw)
    }

    fun saveGlobalProfile(context: Context, profile: DeviceProfile) {
        getPrefs(context).edit().putString(KEY_GLOBAL, profile.toJson()).apply()
    }

    fun getAppProfile(context: Context, pkg: String): DeviceProfile? {
        val raw = getPrefs(context).getString(KEY_PREFIX_APP + pkg, null) ?: return null
        return DeviceProfile.fromJson(raw)
    }

    fun saveAppProfile(context: Context, pkg: String, profile: DeviceProfile) {
        getPrefs(context).edit().putString(KEY_PREFIX_APP + pkg, profile.toJson()).apply()
    }

    fun clearAppProfile(context: Context, pkg: String) {
        getPrefs(context).edit().remove(KEY_PREFIX_APP + pkg).apply()
    }

    fun getEnabledApps(context: Context): MutableSet<String> {
        return (getPrefs(context).getStringSet(KEY_ENABLED_APPS, emptySet()) ?: emptySet()).toMutableSet()
    }

    fun setAppEnabled(context: Context, pkg: String, enabled: Boolean) {
        val set = getEnabledApps(context)
        if (enabled) set.add(pkg) else set.remove(pkg)
        getPrefs(context).edit().putStringSet(KEY_ENABLED_APPS, set).apply()
    }

    fun isAutoClearEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_CLEAR, false)
    }

    fun setAutoClearEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CLEAR, enabled).apply()
    }

    fun getClearDataApps(context: Context): MutableSet<String> {
        return (getPrefs(context).getStringSet(KEY_CLEAR_APPS, emptySet()) ?: emptySet()).toMutableSet()
    }

    fun setClearDataApps(context: Context, apps: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_CLEAR_APPS, apps).apply()
    }
}
