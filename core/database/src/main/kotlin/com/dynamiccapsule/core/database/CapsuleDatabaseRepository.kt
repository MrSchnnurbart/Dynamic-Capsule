package com.dynamiccapsule.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dynamiccapsule.core.model.CameraCutoutPlacement
import com.dynamiccapsule.core.model.ScreenOrientationMode
import com.dynamiccapsule.core.model.ScreenProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class AllowedNotificationAppRule(
    val packageName: String,
    val appLabel: String,
    val enabled: Boolean = true,
    val customAccentColorArgb: Long = 0xFF30D158L
)

@Serializable
data class SavedBarcodeItem(
    val id: Long = 0L,
    val label: String,
    val content: String,
    val isBarcode128: Boolean = false
)

class CapsuleDatabaseRepository(
    context: Context? = null,
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val dbHelper: CapsuleSqliteHelper? = context?.applicationContext?.let {
        CapsuleSqliteHelper(it)
    }

    private val _screenProfiles = MutableStateFlow<Map<String, ScreenProfile>>(emptyMap())
    val screenProfiles: StateFlow<Map<String, ScreenProfile>> = _screenProfiles.asStateFlow()

    private val _allowedApps = MutableStateFlow<Map<String, AllowedNotificationAppRule>>(emptyMap())
    val allowedApps: StateFlow<Map<String, AllowedNotificationAppRule>> = _allowedApps.asStateFlow()

    private val _savedBarcodes = MutableStateFlow<List<SavedBarcodeItem>>(emptyList())
    val savedBarcodes: StateFlow<List<SavedBarcodeItem>> = _savedBarcodes.asStateFlow()

    init {
        // Initialise immédiatement en mémoire pour ne jamais bloquer le thread principal
        seedDefaultsInMemory()
        if (dbHelper != null) {
            ioScope.launch {
                loadFromSqlite()
            }
        }
    }

    private fun seedDefaultsInMemory() {
        val defaultPortrait = ScreenProfile(
            profileKey = ScreenProfile.buildKey(1080, 2400, ScreenOrientationMode.PORTRAIT),
            resolutionWidth = 1080,
            resolutionHeight = 2400,
            orientation = ScreenOrientationMode.PORTRAIT,
            xOffsetPx = 0,
            yOffsetPx = 12,
            compactWidthDp = 124,
            compactHeightDp = 34,
            expandedWidthDp = 362,
            expandedHeightDp = 184,
            cornerRadiusDp = 32,
            cutoutPaddingDp = 8,
            cameraPlacement = CameraCutoutPlacement.CENTER,
            cameraCutoutWidthDp = 32,
            cameraCutoutHeightDp = 28,
            cameraSafePaddingDp = 8
        )
        val defaultLandscape = ScreenProfile(
            profileKey = ScreenProfile.buildKey(2400, 1080, ScreenOrientationMode.LANDSCAPE),
            resolutionWidth = 2400,
            resolutionHeight = 1080,
            orientation = ScreenOrientationMode.LANDSCAPE,
            xOffsetPx = 0,
            yOffsetPx = 10,
            compactWidthDp = 136,
            compactHeightDp = 34,
            expandedWidthDp = 396,
            expandedHeightDp = 172,
            cornerRadiusDp = 30,
            cutoutPaddingDp = 8,
            cameraPlacement = CameraCutoutPlacement.LEFT,
            cameraCutoutWidthDp = 32,
            cameraCutoutHeightDp = 28,
            cameraSafePaddingDp = 8
        )
        _screenProfiles.value = mapOf(
            defaultPortrait.profileKey to defaultPortrait,
            defaultLandscape.profileKey to defaultLandscape
        )
        _allowedApps.value = DEFAULT_MESSAGING_APPS.associateBy { it.packageName }
        _savedBarcodes.value = DEFAULT_BARCODES
    }

    private fun loadFromSqlite() {
        val helper = dbHelper ?: return
        runCatching {
            val db = helper.writableDatabase
            val profilesMap = mutableMapOf<String, ScreenProfile>()
            db.query("screen_profiles", null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val key = cursor.getString(cursor.getColumnIndexOrThrow("profile_key"))
                    val width = cursor.getInt(cursor.getColumnIndexOrThrow("res_width"))
                    val height = cursor.getInt(cursor.getColumnIndexOrThrow("res_height"))
                    val orientationStr = cursor.getString(cursor.getColumnIndexOrThrow("orientation"))
                    val orientation = runCatching { ScreenOrientationMode.valueOf(orientationStr) }
                        .getOrDefault(ScreenOrientationMode.PORTRAIT)
                    val placementStr = cursor.getColumnIndex("camera_placement").takeIf { it >= 0 }
                        ?.let { cursor.getString(it) } ?: CameraCutoutPlacement.CENTER.name
                    val placement = runCatching { CameraCutoutPlacement.valueOf(placementStr) }
                        .getOrDefault(CameraCutoutPlacement.CENTER)
                    val camW = cursor.getColumnIndex("camera_width_dp").takeIf { it >= 0 }
                        ?.let { cursor.getInt(it) } ?: 32
                    val camH = cursor.getColumnIndex("camera_height_dp").takeIf { it >= 0 }
                        ?.let { cursor.getInt(it) } ?: 28
                    val camPad = cursor.getColumnIndex("camera_safe_pad_dp").takeIf { it >= 0 }
                        ?.let { cursor.getInt(it) } ?: 8

                    val profile = ScreenProfile(
                        profileKey = key,
                        resolutionWidth = width,
                        resolutionHeight = height,
                        orientation = orientation,
                        xOffsetPx = cursor.getInt(cursor.getColumnIndexOrThrow("x_offset_px")),
                        yOffsetPx = cursor.getInt(cursor.getColumnIndexOrThrow("y_offset_px")),
                        compactWidthDp = cursor.getInt(cursor.getColumnIndexOrThrow("compact_width_dp")),
                        compactHeightDp = cursor.getInt(cursor.getColumnIndexOrThrow("compact_height_dp")),
                        expandedWidthDp = cursor.getInt(cursor.getColumnIndexOrThrow("expanded_width_dp")),
                        expandedHeightDp = cursor.getInt(cursor.getColumnIndexOrThrow("expanded_height_dp")),
                        cornerRadiusDp = cursor.getInt(cursor.getColumnIndexOrThrow("corner_radius_dp")),
                        cutoutPaddingDp = cursor.getInt(cursor.getColumnIndexOrThrow("cutout_padding_dp")),
                        cameraPlacement = placement,
                        cameraCutoutWidthDp = camW,
                        cameraCutoutHeightDp = camH,
                        cameraSafePaddingDp = camPad
                    )
                    profilesMap[key] = profile
                }
            }
            if (profilesMap.isNotEmpty()) {
                _screenProfiles.value = _screenProfiles.value + profilesMap
            } else {
                _screenProfiles.value.values.forEach { persistProfileToSqliteSync(it) }
            }

            val appsMap = mutableMapOf<String, AllowedNotificationAppRule>()
            db.query("allowed_notification_apps", null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val pkg = cursor.getString(cursor.getColumnIndexOrThrow("package_name"))
                    val label = cursor.getString(cursor.getColumnIndexOrThrow("app_label"))
                    val enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1
                    val color = cursor.getLong(cursor.getColumnIndexOrThrow("accent_color"))
                    appsMap[pkg] = AllowedNotificationAppRule(pkg, label, enabled, color)
                }
            }
            if (appsMap.isNotEmpty()) {
                _allowedApps.value = _allowedApps.value + appsMap
            } else {
                DEFAULT_MESSAGING_APPS.forEach { persistAppRuleToSqliteSync(it) }
            }

            val barcodes = mutableListOf<SavedBarcodeItem>()
            db.query("saved_barcodes", null, null, null, null, null, "id ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    barcodes.add(
                        SavedBarcodeItem(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            label = cursor.getString(cursor.getColumnIndexOrThrow("label")),
                            content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                            isBarcode128 = cursor.getInt(cursor.getColumnIndexOrThrow("is_barcode_128")) == 1
                        )
                    )
                }
            }
            if (barcodes.isNotEmpty()) {
                _savedBarcodes.value = barcodes
            }
        }
    }

    fun getOrCreateProfileForDisplay(
        widthPx: Int,
        heightPx: Int,
        orientation: ScreenOrientationMode,
        defaultCutoutTopPx: Int = 12
    ): ScreenProfile {
        val key = ScreenProfile.buildKey(widthPx, heightPx, orientation)
        val existing = _screenProfiles.value[key]
        if (existing != null) return existing

        val created = ScreenProfile(
            profileKey = key,
            resolutionWidth = widthPx,
            resolutionHeight = heightPx,
            orientation = orientation,
            xOffsetPx = 0,
            yOffsetPx = defaultCutoutTopPx,
            compactWidthDp = if (orientation == ScreenOrientationMode.PORTRAIT) 124 else 138,
            compactHeightDp = 34,
            expandedWidthDp = if (orientation == ScreenOrientationMode.PORTRAIT) 362 else 396,
            expandedHeightDp = if (orientation == ScreenOrientationMode.PORTRAIT) 184 else 172,
            cornerRadiusDp = 32,
            cutoutPaddingDp = 8,
            cameraPlacement = if (orientation == ScreenOrientationMode.PORTRAIT) {
                CameraCutoutPlacement.CENTER
            } else {
                CameraCutoutPlacement.LEFT
            },
            cameraCutoutWidthDp = 32,
            cameraCutoutHeightDp = 28,
            cameraSafePaddingDp = 8
        )
        saveScreenProfile(created)
        return created
    }

    fun saveScreenProfile(profile: ScreenProfile) {
        _screenProfiles.update { current ->
            current.toMutableMap().apply { put(profile.profileKey, profile) }
        }
        if (dbHelper != null) {
            ioScope.launch { persistProfileToSqliteSync(profile) }
        }
    }

    fun deleteScreenProfile(profileKey: String) {
        _screenProfiles.update { current ->
            current.toMutableMap().apply { remove(profileKey) }
        }
        if (dbHelper != null) {
            ioScope.launch {
                runCatching {
                    dbHelper.writableDatabase.delete("screen_profiles", "profile_key = ?", arrayOf(profileKey))
                }
            }
        }
    }

    fun setAppNotificationAllowed(
        packageName: String,
        appLabel: String,
        enabled: Boolean,
        accentColorArgb: Long = 0xFF30D158L
    ) {
        val rule = AllowedNotificationAppRule(
            packageName = packageName,
            appLabel = appLabel,
            enabled = enabled,
            customAccentColorArgb = accentColorArgb
        )
        _allowedApps.update { current ->
            current.toMutableMap().apply { put(packageName, rule) }
        }
        if (dbHelper != null) {
            ioScope.launch { persistAppRuleToSqliteSync(rule) }
        }
    }

    fun isAppNotificationAllowed(packageName: String): Boolean =
        _allowedApps.value[packageName]?.enabled ?: false

    fun addOrUpdateBarcode(label: String, content: String, isBarcode128: Boolean) {
        val nextId = (_savedBarcodes.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val item = SavedBarcodeItem(id = nextId, label = label, content = content, isBarcode128 = isBarcode128)
        _savedBarcodes.update { current -> current + item }
        if (dbHelper != null) {
            ioScope.launch { persistBarcodeToSqliteSync(item) }
        }
    }

    fun removeBarcode(id: Long) {
        _savedBarcodes.update { list -> list.filterNot { it.id == id } }
        if (dbHelper != null) {
            ioScope.launch {
                runCatching {
                    dbHelper.writableDatabase.delete("saved_barcodes", "id = ?", arrayOf(id.toString()))
                }
            }
        }
    }

    private fun persistProfileToSqliteSync(profile: ScreenProfile) {
        val helper = dbHelper ?: return
        runCatching {
            val values = ContentValues().apply {
                put("profile_key", profile.profileKey)
                put("res_width", profile.resolutionWidth)
                put("res_height", profile.resolutionHeight)
                put("orientation", profile.orientation.name)
                put("x_offset_px", profile.xOffsetPx)
                put("y_offset_px", profile.yOffsetPx)
                put("compact_width_dp", profile.compactWidthDp)
                put("compact_height_dp", profile.compactHeightDp)
                put("expanded_width_dp", profile.expandedWidthDp)
                put("expanded_height_dp", profile.expandedHeightDp)
                put("corner_radius_dp", profile.cornerRadiusDp)
                put("cutout_padding_dp", profile.cutoutPaddingDp)
                put("camera_placement", profile.cameraPlacement.name)
                put("camera_width_dp", profile.cameraCutoutWidthDp)
                put("camera_height_dp", profile.cameraCutoutHeightDp)
                put("camera_safe_pad_dp", profile.cameraSafePaddingDp)
            }
            helper.writableDatabase.insertWithOnConflict(
                "screen_profiles",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun persistAppRuleToSqliteSync(rule: AllowedNotificationAppRule) {
        val helper = dbHelper ?: return
        runCatching {
            val values = ContentValues().apply {
                put("package_name", rule.packageName)
                put("app_label", rule.appLabel)
                put("enabled", if (rule.enabled) 1 else 0)
                put("accent_color", rule.customAccentColorArgb)
            }
            helper.writableDatabase.insertWithOnConflict(
                "allowed_notification_apps",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun persistBarcodeToSqliteSync(item: SavedBarcodeItem): Long {
        val helper = dbHelper ?: return item.id
        return runCatching {
            val values = ContentValues().apply {
                put("label", item.label)
                put("content", item.content)
                put("is_barcode_128", if (item.isBarcode128) 1 else 0)
            }
            helper.writableDatabase.insert("saved_barcodes", null, values)
        }.getOrDefault(item.id)
    }

    companion object {
        val DEFAULT_MESSAGING_APPS = listOf(
            AllowedNotificationAppRule("com.whatsapp", "WhatsApp", true, 0xFF30D158L),
            AllowedNotificationAppRule("org.telegram.messenger", "Telegram", true, 0xFF0A84FFL),
            AllowedNotificationAppRule("com.google.android.apps.messaging", "Messages", true, 0xFF0A84FFL),
            AllowedNotificationAppRule("com.discord", "Discord", true, 0xFF5E5CE6L),
            AllowedNotificationAppRule("org.thoughtcrime.securesms", "Signal", true, 0xFF0A84FFL),
            AllowedNotificationAppRule("com.instagram.android", "Instagram", true, 0xFFFF375FL),
            AllowedNotificationAppRule("com.google.android.gm", "Gmail", false, 0xFFFF453AL)
        )

        val DEFAULT_BARCODES = listOf(
            SavedBarcodeItem(1L, "Carte Fidélité", "978020137962", isBarcode128 = true),
            SavedBarcodeItem(2L, "Wi-Fi Invité (QR)", "WIFI:S:DynamicCapsule_5G;T:WPA;P:Capsule2026;;", isBarcode128 = false),
            SavedBarcodeItem(3L, "Badge Accès", "PASS-849201", isBarcode128 = true)
        )
    }
}

private class CapsuleSqliteHelper(context: Context) : SQLiteOpenHelper(
    context,
    "dynamic_capsule.db",
    null,
    2
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS screen_profiles (
                profile_key TEXT PRIMARY KEY,
                res_width INTEGER NOT NULL,
                res_height INTEGER NOT NULL,
                orientation TEXT NOT NULL,
                x_offset_px INTEGER NOT NULL,
                y_offset_px INTEGER NOT NULL,
                compact_width_dp INTEGER NOT NULL,
                compact_height_dp INTEGER NOT NULL,
                expanded_width_dp INTEGER NOT NULL,
                expanded_height_dp INTEGER NOT NULL,
                corner_radius_dp INTEGER NOT NULL,
                cutout_padding_dp INTEGER NOT NULL,
                camera_placement TEXT NOT NULL DEFAULT 'CENTER',
                camera_width_dp INTEGER NOT NULL DEFAULT 32,
                camera_height_dp INTEGER NOT NULL DEFAULT 28,
                camera_safe_pad_dp INTEGER NOT NULL DEFAULT 8
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS allowed_notification_apps (
                package_name TEXT PRIMARY KEY,
                app_label TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                accent_color INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_barcodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                label TEXT NOT NULL,
                content TEXT NOT NULL,
                is_barcode_128 INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching { db.execSQL("ALTER TABLE screen_profiles ADD COLUMN camera_placement TEXT NOT NULL DEFAULT 'CENTER'") }
            runCatching { db.execSQL("ALTER TABLE screen_profiles ADD COLUMN camera_width_dp INTEGER NOT NULL DEFAULT 32") }
            runCatching { db.execSQL("ALTER TABLE screen_profiles ADD COLUMN camera_height_dp INTEGER NOT NULL DEFAULT 28") }
            runCatching { db.execSQL("ALTER TABLE screen_profiles ADD COLUMN camera_safe_pad_dp INTEGER NOT NULL DEFAULT 8") }
        }
    }
}
