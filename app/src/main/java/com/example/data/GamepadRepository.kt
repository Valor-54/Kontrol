package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GamepadButton
import com.example.model.GamepadProfile
import com.example.model.InputDriverType
import com.example.model.JoystickConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GamepadRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("flexpad_gamepad_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _profiles = MutableStateFlow<List<GamepadProfile>>(emptyList())
    val profiles: StateFlow<List<GamepadProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(DefaultProfiles.getProfile1())
    val activeProfile: StateFlow<GamepadProfile> = _activeProfile.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _areControlsVisible = MutableStateFlow(true)
    val areControlsVisible: StateFlow<Boolean> = _areControlsVisible.asStateFlow()

    private val _inputDriver = MutableStateFlow(InputDriverType.ACCESSIBILITY)
    val inputDriver: StateFlow<InputDriverType> = _inputDriver.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val savedProfilesJson = prefs.getString(KEY_PROFILES_JSON, null)
        val loadedProfiles = if (savedProfilesJson.isNullOrBlank()) {
            DefaultProfiles.getAllDefaultProfiles()
        } else {
            GamepadJsonSerializer.deserializeProfiles(savedProfilesJson)
        }
        _profiles.value = loadedProfiles

        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, loadedProfiles.first().id)
        val active = loadedProfiles.find { it.id == activeId } ?: loadedProfiles.first()
        _activeProfile.value = active

        val driverName = prefs.getString(KEY_DRIVER, InputDriverType.ACCESSIBILITY.name)
        _inputDriver.value = try {
            InputDriverType.valueOf(driverName ?: InputDriverType.ACCESSIBILITY.name)
        } catch (e: Exception) {
            InputDriverType.ACCESSIBILITY
        }
    }

    fun selectProfile(profileId: String) {
        val target = _profiles.value.find { it.id == profileId } ?: return
        _activeProfile.value = target
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun toggleControlsVisibility() {
        _areControlsVisible.value = !_areControlsVisible.value
    }

    fun setControlsVisibility(visible: Boolean) {
        _areControlsVisible.value = visible
    }

    fun revertToSnapshot(snapshot: GamepadProfile) {
        updateActiveProfile(snapshot)
    }

    fun setInputDriver(driver: InputDriverType) {
        _inputDriver.value = driver
        prefs.edit().putString(KEY_DRIVER, driver.name).apply()
    }

    fun updateActiveProfile(updated: GamepadProfile) {
        _activeProfile.value = updated
        val updatedList = _profiles.value.map {
            if (it.id == updated.id) updated else it
        }
        _profiles.value = updatedList
        saveProfiles(updatedList)
    }

    fun updateLeftJoystick(updater: (JoystickConfig) -> JoystickConfig) {
        val current = _activeProfile.value
        val newJoy = updater(current.leftJoystick)
        updateActiveProfile(current.copy(leftJoystick = newJoy))
    }

    fun updateRightJoystick(updater: (JoystickConfig) -> JoystickConfig) {
        val current = _activeProfile.value
        val newJoy = updater(current.rightJoystick)
        updateActiveProfile(current.copy(rightJoystick = newJoy))
    }

    fun updateButton(buttonId: String, updater: (GamepadButton) -> GamepadButton) {
        val current = _activeProfile.value
        val updatedButtons = current.buttons.map {
            if (it.id == buttonId) updater(it) else it
        }
        updateActiveProfile(current.copy(buttons = updatedButtons))
    }

    fun addButton(newButton: GamepadButton) {
        val current = _activeProfile.value
        val updatedButtons = current.buttons + newButton
        updateActiveProfile(current.copy(buttons = updatedButtons))
    }

    fun deleteButton(buttonId: String) {
        val current = _activeProfile.value
        val updatedButtons = current.buttons.filter { it.id != buttonId }
        updateActiveProfile(current.copy(buttons = updatedButtons))
    }

    fun resetActiveProfileToDefault() {
        val currentId = _activeProfile.value.id
        val defaultProfile = when (currentId) {
            "profile_1_dual_gamepad" -> DefaultProfiles.getProfile1()
            "profile_2_fps_shooter" -> DefaultProfiles.getProfile2()
            "profile_3_retro_arcade" -> DefaultProfiles.getProfile3()
            else -> DefaultProfiles.getProfile1().copy(id = currentId)
        }
        updateActiveProfile(defaultProfile)
    }

    private fun saveProfiles(list: List<GamepadProfile>) {
        scope.launch {
            val json = GamepadJsonSerializer.serializeProfiles(list)
            prefs.edit().putString(KEY_PROFILES_JSON, json).apply()
        }
    }

    companion object {
        private const val KEY_PROFILES_JSON = "key_profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "key_active_profile_id"
        private const val KEY_DRIVER = "key_input_driver"

        @Volatile
        private var INSTANCE: GamepadRepository? = null

        fun getInstance(context: Context): GamepadRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamepadRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
