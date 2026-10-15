package com.example.data

import android.view.KeyEvent
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.GamepadProfile
import com.example.model.JoystickConfig
import com.example.model.JoystickMode
import org.json.JSONArray
import org.json.JSONObject

object GamepadJsonSerializer {

    fun serializeProfiles(profiles: List<GamepadProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(serializeProfile(profile))
        }
        return array.toString()
    }

    fun deserializeProfiles(json: String): List<GamepadProfile> {
        if (json.isBlank()) return DefaultProfiles.getAllDefaultProfiles()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<GamepadProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(deserializeProfile(obj))
            }
            if (list.isEmpty()) DefaultProfiles.getAllDefaultProfiles() else list
        } catch (e: Exception) {
            e.printStackTrace()
            DefaultProfiles.getAllDefaultProfiles()
        }
    }

    fun serializeProfile(profile: GamepadProfile): JSONObject {
        return JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("description", profile.description)
            put("leftJoystick", serializeJoystick(profile.leftJoystick))
            put("rightJoystick", serializeJoystick(profile.rightJoystick))
            put("buttons", JSONArray().apply {
                profile.buttons.forEach { put(serializeButton(it)) }
            })
            put("globalAlpha", profile.globalAlpha.toDouble())
            put("showLabels", profile.showLabels)
            put("vibrationFeedback", profile.vibrationFeedback)
        }
    }

    fun deserializeProfile(obj: JSONObject): GamepadProfile {
        val buttonsArray = obj.optJSONArray("buttons") ?: JSONArray()
        val buttonsList = mutableListOf<GamepadButton>()
        for (i in 0 until buttonsArray.length()) {
            buttonsList.add(deserializeButton(buttonsArray.getJSONObject(i)))
        }

        return GamepadProfile(
            id = obj.optString("id", "profile_1_dual_gamepad"),
            name = obj.optString("name", "Custom Profile"),
            description = obj.optString("description", ""),
            leftJoystick = deserializeJoystick(obj.getJSONObject("leftJoystick")),
            rightJoystick = deserializeJoystick(obj.getJSONObject("rightJoystick")),
            buttons = buttonsList,
            globalAlpha = obj.optDouble("globalAlpha", 0.85).toFloat(),
            showLabels = obj.optBoolean("showLabels", true),
            vibrationFeedback = obj.optBoolean("vibrationFeedback", true)
        )
    }

    private fun serializeJoystick(joy: JoystickConfig): JSONObject {
        return JSONObject().apply {
            put("id", joy.id)
            put("label", joy.label)
            put("centerXRatio", joy.centerXRatio.toDouble())
            put("centerYRatio", joy.centerYRatio.toDouble())
            put("sizeDp", joy.sizeDp.toDouble())
            put("deadzone", joy.deadzone.toDouble())
            put("sensitivity", joy.sensitivity.toDouble())
            put("colorHex", joy.colorHex)
            put("alpha", joy.alpha.toDouble())
            put("targetTouchRadiusRatio", joy.targetTouchRadiusRatio.toDouble())
            put("enabled", joy.enabled)
            put("mode", joy.mode.name)
            put("upKeyCode", joy.upKeyCode)
            put("upKeyLabel", joy.upKeyLabel)
            put("downKeyCode", joy.downKeyCode)
            put("downKeyLabel", joy.downKeyLabel)
            put("leftKeyCode", joy.leftKeyCode)
            put("leftKeyLabel", joy.leftKeyLabel)
            put("rightKeyCode", joy.rightKeyCode)
            put("rightKeyLabel", joy.rightKeyLabel)
        }
    }

    private fun deserializeJoystick(obj: JSONObject): JoystickConfig {
        return JoystickConfig(
            id = obj.optString("id", "joy"),
            label = obj.optString("label", "STICK"),
            centerXRatio = obj.optDouble("centerXRatio", 0.5).toFloat(),
            centerYRatio = obj.optDouble("centerYRatio", 0.5).toFloat(),
            sizeDp = obj.optDouble("sizeDp", 140.0).toFloat(),
            deadzone = obj.optDouble("deadzone", 0.12).toFloat(),
            sensitivity = obj.optDouble("sensitivity", 1.0).toFloat(),
            colorHex = obj.optLong("colorHex", 0xFF00F5D4),
            alpha = obj.optDouble("alpha", 0.8).toFloat(),
            targetTouchRadiusRatio = obj.optDouble("targetTouchRadiusRatio", 0.12).toFloat(),
            enabled = obj.optBoolean("enabled", true),
            mode = try {
                JoystickMode.valueOf(obj.optString("mode", "ANALOG"))
            } catch (e: Exception) {
                JoystickMode.ANALOG
            },
            upKeyCode = obj.optInt("upKeyCode", KeyEvent.KEYCODE_W),
            upKeyLabel = obj.optString("upKeyLabel", "W"),
            downKeyCode = obj.optInt("downKeyCode", KeyEvent.KEYCODE_S),
            downKeyLabel = obj.optString("downKeyLabel", "S"),
            leftKeyCode = obj.optInt("leftKeyCode", KeyEvent.KEYCODE_A),
            leftKeyLabel = obj.optString("leftKeyLabel", "A"),
            rightKeyCode = obj.optInt("rightKeyCode", KeyEvent.KEYCODE_D),
            rightKeyLabel = obj.optString("rightKeyLabel", "D")
        )
    }

    private fun serializeButton(btn: GamepadButton): JSONObject {
        return JSONObject().apply {
            put("id", btn.id)
            put("label", btn.label)
            put("xRatio", btn.xRatio.toDouble())
            put("yRatio", btn.yRatio.toDouble())
            put("sizeDp", btn.sizeDp.toDouble())
            put("shape", btn.shape.name)
            put("colorHex", btn.colorHex)
            put("alpha", btn.alpha.toDouble())
            put("actionType", btn.actionType.name)
            put("keyCode", btn.keyCode)
            put("targetTouchXRatio", btn.targetTouchXRatio.toDouble())
            put("targetTouchYRatio", btn.targetTouchYRatio.toDouble())
        }
    }

    private fun deserializeButton(obj: JSONObject): GamepadButton {
        return GamepadButton(
            id = obj.optString("id", "btn"),
            label = obj.optString("label", "A"),
            xRatio = obj.optDouble("xRatio", 0.5).toFloat(),
            yRatio = obj.optDouble("yRatio", 0.5).toFloat(),
            sizeDp = obj.optDouble("sizeDp", 60.0).toFloat(),
            shape = try { ButtonShape.valueOf(obj.optString("shape", "CIRCLE")) } catch (e: Exception) { ButtonShape.CIRCLE },
            colorHex = obj.optLong("colorHex", 0xFF2D2E36),
            alpha = obj.optDouble("alpha", 0.85).toFloat(),
            actionType = try { ActionType.valueOf(obj.optString("actionType", "KEY_CODE")) } catch (e: Exception) { ActionType.KEY_CODE },
            keyCode = obj.optInt("keyCode", KeyEvent.KEYCODE_BUTTON_A),
            targetTouchXRatio = obj.optDouble("targetTouchXRatio", 0.5).toFloat(),
            targetTouchYRatio = obj.optDouble("targetTouchYRatio", 0.5).toFloat()
        )
    }
}
