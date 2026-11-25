package com.devicefaker.neo.xposed

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import com.devicefaker.neo.data.DeviceProfile
import com.devicefaker.neo.data.PrefsStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.net.NetworkInterface

class FakerHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName ?: return

        // Self-hook so UI knows LSPosed module is active
        if (pkg == "com.devicefaker.neo") {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.devicefaker.neo.ui.MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (t: Throwable) {
                XposedBridge.log("DeviceFakerNeo: self-hook failed: $t")
            }
            return
        }

        val prefs = XSharedPreferences("com.devicefaker.neo", PrefsStore.PREFS_NAME)
        prefs.makeWorldReadable()
        prefs.reload()

        // Priority: Per-App profile -> Global profile
        val perAppRaw = prefs.getString(PrefsStore.KEY_PREFIX_APP + pkg, null)
        val profile = if (!perAppRaw.isNullOrBlank()) {
            DeviceProfile.fromJson(perAppRaw)
        } else {
            val enabledApps = prefs.getStringSet(PrefsStore.KEY_ENABLED_APPS, emptySet()) ?: emptySet()
            // If user selected specific apps inside UI, respect it; if empty, apply global to all LSPosed-scoped apps
            if (enabledApps.isNotEmpty() && !enabledApps.contains(pkg)) return
            val globalRaw = prefs.getString(PrefsStore.KEY_GLOBAL, null) ?: return
            DeviceProfile.fromJson(globalRaw)
        }

        if (!profile.enabled) return

        XposedBridge.log("DeviceFakerNeo: Hooking $pkg as ${profile.brand} ${profile.model}")

        hookBuild(lpparam.classLoader, profile)
        hookSystemProperties(lpparam.classLoader, profile)
        hookAndroidId(lpparam.classLoader, profile)
        hookTelephony(lpparam.classLoader, profile)
        hookWifiAndBluetooth(lpparam.classLoader, profile)
        hookGsfId(lpparam.classLoader, profile)
        hookGaid(lpparam.classLoader, profile)
    }

    private fun hookBuild(cl: ClassLoader, p: DeviceProfile) {
        try {
            if (p.manufacturer.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "MANUFACTURER", p.manufacturer)
            if (p.brand.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "BRAND", p.brand)
            if (p.model.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "MODEL", p.model)
            if (p.device.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "DEVICE", p.device)
            if (p.product.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "PRODUCT", p.product)
            if (p.board.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "BOARD", p.board)
            if (p.hardware.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "HARDWARE", p.hardware)
            if (p.fingerprint.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "FINGERPRINT", p.fingerprint)
            if (p.serial.isNotBlank()) {
                XposedHelpers.setStaticObjectField(Build::class.java, "SERIAL", p.serial)
                try {
                    XposedHelpers.findAndHookMethod(
                        Build::class.java, "getSerial",
                        XC_MethodReplacement.returnConstant(p.serial)
                    )
                } catch (_: Throwable) {}
            }
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    if (p.socModel.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "SOC_MODEL", p.socModel)
                    if (p.socManufacturer.isNotBlank()) XposedHelpers.setStaticObjectField(Build::class.java, "SOC_MANUFACTURER", p.socManufacturer)
                } catch (_: Throwable) {}
            }
            if (p.release.isNotBlank()) {
                XposedHelpers.setStaticObjectField(Build.VERSION::class.java, "RELEASE", p.release)
            }
            val sdk = p.sdkInt.toIntOrNull()
            if (sdk != null && sdk in 21..36) {
                XposedHelpers.setStaticIntField(Build.VERSION::class.java, "SDK_INT", sdk)
            }
        } catch (t: Throwable) {
            XposedBridge.log("DeviceFakerNeo: hookBuild err: $t")
        }
    }

    private fun hookSystemProperties(cl: ClassLoader, p: DeviceProfile) {
        val map = mutableMapOf<String, String>()
        fun put(k: String, v: String) { if (v.isNotBlank()) map[k] = v }

        put("ro.product.manufacturer", p.manufacturer)
        put("ro.product.system.manufacturer", p.manufacturer)
        put("ro.product.vendor.manufacturer", p.manufacturer)
        put("ro.product.brand", p.brand)
        put("ro.product.system.brand", p.brand)
        put("ro.product.vendor.brand", p.brand)
        put("ro.product.model", p.model)
        put("ro.product.system.model", p.model)
        put("ro.product.vendor.model", p.model)
        put("ro.product.device", p.device)
        put("ro.product.name", p.product)
        put("ro.product.board", p.board)
        put("ro.hardware", p.hardware)
        put("ro.soc.model", p.socModel)
        put("ro.soc.manufacturer", p.socManufacturer)
        put("ro.build.fingerprint", p.fingerprint)
        put("ro.system.build.fingerprint", p.fingerprint)
        put("ro.vendor.build.fingerprint", p.fingerprint)
        put("ro.build.version.release", p.release)
        put("ro.serialno", p.serial)
        put("gsm.sim.operator.numeric", p.simOperator)
        put("gsm.operator.numeric", p.simOperator)
        put("gsm.sim.operator.iso-country", p.simCountry)
        put("gsm.operator.iso-country", p.simCountry)
        put("gsm.sim.operator.alpha", p.operatorName)
        put("gsm.operator.alpha", p.operatorName)

        if (map.isEmpty()) return

        try {
            val spClass = XposedHelpers.findClass("android.os.SystemProperties", cl)
            XposedHelpers.findAndHookMethod(
                spClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        map[key]?.let { param.result = it }
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                spClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        map[key]?.let { param.result = it }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("DeviceFakerNeo: hookSystemProperties err: $t")
        }
    }

    private fun hookAndroidId(cl: ClassLoader, p: DeviceProfile) {
        if (p.androidId.isBlank()) return
        try {
            val secureClass = XposedHelpers.findClass("android.provider.Settings\$Secure", cl)
            val hook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.size >= 2 && param.args[1] == "android_id") {
                        param.result = p.androidId
                    }
                }
            }
            XposedHelpers.findAndHookMethod(
                secureClass, "getString",
                ContentResolver::class.java, String::class.java, hook
            )
            try {
                XposedHelpers.findAndHookMethod(
                    secureClass, "getStringForUser",
                    ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType, hook
                )
            } catch (_: Throwable) {}
        } catch (t: Throwable) {
            XposedBridge.log("DeviceFakerNeo: hookAndroidId err: $t")
        }
    }

    private fun hookTelephony(cl: ClassLoader, p: DeviceProfile) {
        try {
            val tmClass = XposedHelpers.findClass("android.telephony.TelephonyManager", cl)
            fun hookAll(method: String, value: String) {
                if (value.isBlank()) return
                try {
                    XposedBridge.hookAllMethods(tmClass, method, XC_MethodReplacement.returnConstant(value))
                } catch (_: Throwable) {}
            }
            hookAll("getImei", p.imei)
            hookAll("getMeid", p.imei)
            hookAll("getDeviceId", p.imei)
            hookAll("getSubscriberId", p.imsi)
            hookAll("getSimSerialNumber", p.iccid)
            hookAll("getLine1Number", p.phone)
            hookAll("getSimOperator", p.simOperator)
            hookAll("getNetworkOperator", p.simOperator)
            hookAll("getSimCountryIso", p.simCountry)
            hookAll("getNetworkCountryIso", p.simCountry)
            hookAll("getSimOperatorName", p.operatorName)
            hookAll("getNetworkOperatorName", p.operatorName)
        } catch (t: Throwable) {
            XposedBridge.log("DeviceFakerNeo: hookTelephony err: $t")
        }
    }

    private fun hookWifiAndBluetooth(cl: ClassLoader, p: DeviceProfile) {
        try {
            val wifiClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", cl)
            if (p.wifiMac.isNotBlank()) {
                XposedBridge.hookAllMethods(wifiClass, "getMacAddress", XC_MethodReplacement.returnConstant(p.wifiMac))
                val macBytes = parseMacBytes(p.wifiMac)
                if (macBytes != null) {
                    XposedHelpers.findAndHookMethod(
                        NetworkInterface::class.java, "getHardwareAddress",
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val ni = param.thisObject as? NetworkInterface ?: return
                                if (ni.name.equals("wlan0", ignoreCase = true)) {
                                    param.result = macBytes.clone()
                                }
                            }
                        }
                    )
                }
            }
            if (p.wifiSsid.isNotBlank()) {
                XposedBridge.hookAllMethods(wifiClass, "getSSID", XC_MethodReplacement.returnConstant("\"${p.wifiSsid}\""))
            }
            if (p.wifiBssid.isNotBlank()) {
                XposedBridge.hookAllMethods(wifiClass, "getBSSID", XC_MethodReplacement.returnConstant(p.wifiBssid))
            }
        } catch (_: Throwable) {}

        if (p.btMac.isNotBlank()) {
            try {
                val btClass = XposedHelpers.findClass("android.bluetooth.BluetoothAdapter", cl)
                XposedBridge.hookAllMethods(btClass, "getAddress", XC_MethodReplacement.returnConstant(p.btMac))
            } catch (_: Throwable) {}
        }
    }

    private fun hookGsfId(cl: ClassLoader, p: DeviceProfile) {
        if (p.gsfId.isBlank()) return
        try {
            XposedBridge.hookAllMethods(
                ContentResolver::class.java, "query",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val uri = param.args.firstOrNull() as? Uri ?: return
                        if (uri.toString().startsWith("content://com.google.android.gsf.gservices")) {
                            val selArgs = param.args.filterIsInstance<Array<String>>().lastOrNull()
                            if (selArgs != null && selArgs.contains("android_id")) {
                                val cursor = MatrixCursor(arrayOf("key", "value"))
                                cursor.addRow(arrayOf("android_id", p.gsfId))
                                param.result = cursor
                            }
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    private fun hookGaid(cl: ClassLoader, p: DeviceProfile) {
        if (p.gaid.isBlank()) return
        try {
            val adInfo = XposedHelpers.findClass("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info", cl)
            XposedBridge.hookAllMethods(adInfo, "getId", XC_MethodReplacement.returnConstant(p.gaid))
        } catch (_: Throwable) {}
    }

    private fun parseMacBytes(mac: String): ByteArray? {
        return try {
            val parts = mac.split(":")
            if (parts.size != 6) return null
            ByteArray(6) { i -> parts[i].toInt(16).toByte() }
        } catch (_: Exception) {
            null
        }
    }
}
