package com.dynamiccapsule.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dynamiccapsule.core.model.AppGeneralSettings
import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.CapsuleAnimationConfig
import com.dynamiccapsule.core.model.CapsuleAppearanceConfig
import com.dynamiccapsule.core.model.CapsuleGestureType
import com.dynamiccapsule.core.model.DynamicCardPriorityItem
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.GestureConfiguration
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.MiniEventCategory
import com.dynamiccapsule.core.model.NotificationVisualConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.capsuleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dynamic_capsule_preferences"
)

class CapsulePreferencesRepository(
    context: Context? = null,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val dataStore: DataStore<Preferences>? = context?.applicationContext?.capsuleDataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "_type"
    }

    private val _isMasterEnabled = MutableStateFlow(true)
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _enabledMiniEvents = MutableStateFlow(
        MiniCapsuleEventType.entries.associateWith { it.defaultEnabled }
    )
    val enabledMiniEvents: StateFlow<Map<MiniCapsuleEventType, Boolean>> = _enabledMiniEvents.asStateFlow()

    private val _dynamicCardPriorities = MutableStateFlow(
        DynamicCardType.entries
            .sortedBy { it.defaultPriority }
            .mapIndexed { index, type ->
                DynamicCardPriorityItem(type = type, enabled = true, priorityOrder = index)
            }
    )
    val dynamicCardPriorities: StateFlow<List<DynamicCardPriorityItem>> = _dynamicCardPriorities.asStateFlow()

    private val _gestureConfig = MutableStateFlow(GestureConfiguration())
    val gestureConfig: StateFlow<GestureConfiguration> = _gestureConfig.asStateFlow()

    private val _appearanceConfig = MutableStateFlow(CapsuleAppearanceConfig())
    val appearanceConfig: StateFlow<CapsuleAppearanceConfig> = _appearanceConfig.asStateFlow()

    private val _animationConfig = MutableStateFlow(CapsuleAnimationConfig())
    val animationConfig: StateFlow<CapsuleAnimationConfig> = _animationConfig.asStateFlow()

    private val _notificationVisualConfig = MutableStateFlow(NotificationVisualConfig())
    val notificationVisualConfig: StateFlow<NotificationVisualConfig> = _notificationVisualConfig.asStateFlow()

    private val _generalSettings = MutableStateFlow(AppGeneralSettings())
    val generalSettings: StateFlow<AppGeneralSettings> = _generalSettings.asStateFlow()

    private val _savedOriginalStatusBarBlacklist = MutableStateFlow<String?>(null)
    val savedOriginalStatusBarBlacklist: StateFlow<String?> = _savedOriginalStatusBarBlacklist.asStateFlow()

    private val _isLoaded = MutableStateFlow(dataStore == null)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        if (dataStore != null) {
            externalScope.launch {
                dataStore.data.collectLatest { prefs ->
                    prefs[KEY_MASTER_ENABLED]?.let { _isMasterEnabled.value = it }
                    prefs[KEY_MINI_EVENTS_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<Map<MiniCapsuleEventType, Boolean>>(raw)
                        }.getOrNull()?.let { decoded ->
                            val merged = MiniCapsuleEventType.entries.associateWith { type ->
                                decoded[type] ?: type.defaultEnabled
                            }
                            _enabledMiniEvents.value = merged
                        }
                    }
                    prefs[KEY_DYNAMIC_CARDS_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<List<DynamicCardPriorityItem>>(raw)
                        }.getOrNull()?.let { decoded ->
                            if (decoded.isNotEmpty()) {
                                _dynamicCardPriorities.value = decoded.sortedBy { it.priorityOrder }
                            }
                        }
                    }
                    prefs[KEY_GESTURE_CONFIG_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<GestureConfiguration>(raw)
                        }.getOrNull()?.let { _gestureConfig.value = it }
                    }
                    prefs[KEY_APPEARANCE_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<CapsuleAppearanceConfig>(raw)
                        }.getOrNull()?.let { _appearanceConfig.value = it }
                    }
                    prefs[KEY_ANIMATION_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<CapsuleAnimationConfig>(raw)
                        }.getOrNull()?.let { _animationConfig.value = it }
                    }
                    prefs[KEY_NOTIFICATION_VISUAL_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<NotificationVisualConfig>(raw)
                        }.getOrNull()?.let { _notificationVisualConfig.value = it }
                    }
                    prefs[KEY_GENERAL_SETTINGS_JSON]?.let { raw ->
                        runCatching {
                            json.decodeFromString<AppGeneralSettings>(raw)
                        }.getOrNull()?.let { _generalSettings.value = it }
                    }
                    _savedOriginalStatusBarBlacklist.value = prefs[KEY_ORIGINAL_ICON_BLACKLIST]
                    _isLoaded.value = true
                }
            }
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        _isMasterEnabled.value = enabled
        persist { it[KEY_MASTER_ENABLED] = enabled }
    }

    fun setMiniEventEnabled(type: MiniCapsuleEventType, enabled: Boolean) {
        _enabledMiniEvents.update { current ->
            current.toMutableMap().apply { put(type, enabled) }
        }
        val encoded = json.encodeToString(_enabledMiniEvents.value)
        persist { it[KEY_MINI_EVENTS_JSON] = encoded }
    }

    fun setCategoryMiniEventsEnabled(category: MiniEventCategory, enabled: Boolean) {
        _enabledMiniEvents.update { current ->
            current.toMutableMap().apply {
                MiniCapsuleEventType.entries
                    .filter { it.category == category }
                    .forEach { put(it, enabled) }
            }
        }
        val encoded = json.encodeToString(_enabledMiniEvents.value)
        persist { it[KEY_MINI_EVENTS_JSON] = encoded }
    }

    fun isMiniEventEnabled(type: MiniCapsuleEventType): Boolean =
        _enabledMiniEvents.value[type] ?: true

    fun updateDynamicCardOrder(reorderedTypes: List<DynamicCardType>) {
        val currentMap = _dynamicCardPriorities.value.associateBy { it.type }
        val updated = reorderedTypes.mapIndexed { index, cardType ->
            val existing = currentMap[cardType]
            DynamicCardPriorityItem(
                type = cardType,
                enabled = existing?.enabled ?: true,
                priorityOrder = index
            )
        }
        _dynamicCardPriorities.value = updated
        val encoded = json.encodeToString(updated)
        persist { it[KEY_DYNAMIC_CARDS_JSON] = encoded }
    }

    fun setDynamicCardEnabled(type: DynamicCardType, enabled: Boolean) {
        _dynamicCardPriorities.update { list ->
            list.map { item ->
                if (item.type == type) item.copy(enabled = enabled) else item
            }
        }
        val encoded = json.encodeToString(_dynamicCardPriorities.value)
        persist { it[KEY_DYNAMIC_CARDS_JSON] = encoded }
    }

    fun updateGestureAction(gesture: CapsuleGestureType, action: CapsuleAction) {
        _gestureConfig.update { current ->
            when (gesture) {
                CapsuleGestureType.SINGLE_TAP -> current.copy(singleTapAction = action)
                CapsuleGestureType.LONG_PRESS -> current.copy(longPressAction = action)
                CapsuleGestureType.DOUBLE_TAP -> current.copy(doubleTapAction = action)
                CapsuleGestureType.SWIPE_LEFT -> current.copy(swipeLeftAction = action)
                CapsuleGestureType.SWIPE_RIGHT -> current.copy(swipeRightAction = action)
            }
        }
        val encoded = json.encodeToString(_gestureConfig.value)
        persist { it[KEY_GESTURE_CONFIG_JSON] = encoded }
    }

    fun updateSwipeParameters(invertDirection: Boolean, sensitivity: Float) {
        _gestureConfig.update { current ->
            current.copy(
                invertSwipeDirection = invertDirection,
                swipeSensitivity = sensitivity.coerceIn(0.4f, 2.0f)
            )
        }
        val encoded = json.encodeToString(_gestureConfig.value)
        persist { it[KEY_GESTURE_CONFIG_JSON] = encoded }
    }

    fun updateAppearanceConfig(transform: (CapsuleAppearanceConfig) -> CapsuleAppearanceConfig) {
        _appearanceConfig.update(transform)
        val encoded = json.encodeToString(_appearanceConfig.value)
        persist { it[KEY_APPEARANCE_JSON] = encoded }
    }

    fun updateAnimationConfig(transform: (CapsuleAnimationConfig) -> CapsuleAnimationConfig) {
        _animationConfig.update(transform)
        val encoded = json.encodeToString(_animationConfig.value)
        persist { it[KEY_ANIMATION_JSON] = encoded }
    }

    fun updateNotificationVisualConfig(transform: (NotificationVisualConfig) -> NotificationVisualConfig) {
        _notificationVisualConfig.update(transform)
        val encoded = json.encodeToString(_notificationVisualConfig.value)
        persist { it[KEY_NOTIFICATION_VISUAL_JSON] = encoded }
    }

    fun updateGeneralSettings(transform: (AppGeneralSettings) -> AppGeneralSettings) {
        _generalSettings.update(transform)
        val encoded = json.encodeToString(_generalSettings.value)
        persist { it[KEY_GENERAL_SETTINGS_JSON] = encoded }
    }

    fun saveOriginalStatusBarBlacklist(value: String?) {
        _savedOriginalStatusBarBlacklist.value = value
        persist { prefs ->
            if (value == null) {
                prefs.remove(KEY_ORIGINAL_ICON_BLACKLIST)
            } else {
                prefs[KEY_ORIGINAL_ICON_BLACKLIST] = value
            }
        }
    }

    private fun persist(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        val ds = dataStore ?: return
        externalScope.launch {
            runCatching { ds.edit(block) }
        }
    }

    companion object {
        private val KEY_MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        private val KEY_MINI_EVENTS_JSON = stringPreferencesKey("mini_events_json")
        private val KEY_DYNAMIC_CARDS_JSON = stringPreferencesKey("dynamic_cards_json")
        private val KEY_GESTURE_CONFIG_JSON = stringPreferencesKey("gesture_config_json")
        private val KEY_APPEARANCE_JSON = stringPreferencesKey("appearance_config_json")
        private val KEY_ANIMATION_JSON = stringPreferencesKey("animation_config_json")
        private val KEY_NOTIFICATION_VISUAL_JSON = stringPreferencesKey("notification_visual_json")
        private val KEY_GENERAL_SETTINGS_JSON = stringPreferencesKey("general_settings_json")
        private val KEY_ORIGINAL_ICON_BLACKLIST = stringPreferencesKey("original_icon_blacklist")
    }
}
