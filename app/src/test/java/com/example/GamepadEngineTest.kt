package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultProfiles
import com.example.data.GamepadJsonSerializer
import com.example.data.GamepadRepository
import com.example.input.InputDispatchEngine
import android.view.KeyEvent
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.InputDriverType
import com.example.model.JoystickConfig
import com.example.model.JoystickMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GamepadEngineTest {

    private lateinit var context: Context
    private lateinit var engine: InputDispatchEngine
    private lateinit var repository: GamepadRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        engine = InputDispatchEngine(context)
        repository = GamepadRepository(context)
    }

    @Test
    fun testDefaultProfilesExistAndValid() {
        val profiles = DefaultProfiles.getAllDefaultProfiles()
        assertEquals(3, profiles.size)

        val p1 = profiles[0]
        assertEquals("profile_1_dual_gamepad", p1.id)
        assertNotNull(p1.leftJoystick)
        assertNotNull(p1.rightJoystick)
        assertTrue(p1.buttons.isNotEmpty())

        val p2 = profiles[1]
        assertEquals("profile_2_fps_shooter", p2.id)

        val p3 = profiles[2]
        assertEquals("profile_3_retro_arcade", p3.id)
    }

    @Test
    fun testProfileSerializationRoundTrip() {
        val original = DefaultProfiles.getProfile1()
        val json = GamepadJsonSerializer.serializeProfile(original)
        val deserialized = GamepadJsonSerializer.deserializeProfile(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.buttons.size, deserialized.buttons.size)
        assertEquals(original.leftJoystick.deadzone, deserialized.leftJoystick.deadzone, 0.001f)
        assertEquals(original.rightJoystick.sensitivity, deserialized.rightJoystick.sensitivity, 0.001f)
    }

    @Test
    fun testJoystickEnabledFlagRoundTrip() {
        val original = DefaultProfiles.getProfile1()
        val disabledJoy = original.copy(leftJoystick = original.leftJoystick.copy(enabled = false))
        val json = GamepadJsonSerializer.serializeProfile(disabledJoy)
        val deserialized = GamepadJsonSerializer.deserializeProfile(json)

        assertFalse(deserialized.leftJoystick.enabled)
        assertTrue(deserialized.rightJoystick.enabled)
    }

    @Test
    fun testSingleJoystickDeadzoneClamping() {
        engine.setDriver(InputDriverType.SIMULATED)

        // Down
        engine.onJoystickDown(isLeft = true, pointerId = 1, centerX = 100f, centerY = 100f)
        assertTrue(engine.liveLeftJoystick.value.isActive)
        assertEquals(0f, engine.liveLeftJoystick.value.normalizedX, 0.01f)

        // Small deflection within deadzone (10% of 100px = 10px, deadzone = 0.15)
        engine.onJoystickMove(
            isLeft = true,
            pointerId = 1,
            deltaX = 8f,
            deltaY = 0f,
            maxRadius = 100f,
            deadzone = 0.15f,
            sensitivity = 1.0f,
            screenCenterX = 100f,
            screenCenterY = 100f
        )
        // Inside deadzone, output must remain strictly 0
        assertEquals(0f, engine.liveLeftJoystick.value.normalizedX, 0.001f)

        // Large deflection exceeding deadzone (80px out of 100px)
        engine.onJoystickMove(
            isLeft = true,
            pointerId = 1,
            deltaX = 80f,
            deltaY = 0f,
            maxRadius = 100f,
            deadzone = 0.15f,
            sensitivity = 1.0f,
            screenCenterX = 100f,
            screenCenterY = 100f
        )
        assertTrue(engine.liveLeftJoystick.value.normalizedX > 0.5f)

        // Release resets to center
        engine.onJoystickUp(isLeft = true, pointerId = 1)
        assertFalse(engine.liveLeftJoystick.value.isActive)
        assertEquals(0f, engine.liveLeftJoystick.value.normalizedX, 0.001f)
    }

    @Test
    fun testSimultaneousDualJoysticksOperation() {
        engine.setDriver(InputDriverType.SIMULATED)

        // Left Joystick at pointer 10, Right Joystick at pointer 20
        engine.onJoystickDown(isLeft = true, pointerId = 10, centerX = 150f, centerY = 500f)
        engine.onJoystickDown(isLeft = false, pointerId = 20, centerX = 850f, centerY = 500f)

        // Move Left Stick Forward-Right
        engine.onJoystickMove(
            isLeft = true,
            pointerId = 10,
            deltaX = 60f,
            deltaY = -60f,
            maxRadius = 100f,
            deadzone = 0.10f,
            sensitivity = 1.0f,
            screenCenterX = 150f,
            screenCenterY = 500f
        )

        // Move Right Stick Backward-Left
        engine.onJoystickMove(
            isLeft = false,
            pointerId = 20,
            deltaX = -70f,
            deltaY = 70f,
            maxRadius = 100f,
            deadzone = 0.10f,
            sensitivity = 1.0f,
            screenCenterX = 850f,
            screenCenterY = 500f
        )

        assertTrue(engine.liveLeftJoystick.value.isActive)
        assertTrue(engine.liveRightJoystick.value.isActive)

        assertTrue(engine.liveLeftJoystick.value.normalizedX > 0f)
        assertTrue(engine.liveLeftJoystick.value.normalizedY < 0f)

        assertTrue(engine.liveRightJoystick.value.normalizedX < 0f)
        assertTrue(engine.liveRightJoystick.value.normalizedY > 0f)

        assertEquals(2, engine.activePointerIds.value.size)
    }

    @Test
    fun testSimultaneousJoystickAndMultipleButtons() {
        engine.setDriver(InputDriverType.SIMULATED)

        val btnA = GamepadButton("btn_a", "A", 0.8f, 0.4f, 60f, ButtonShape.CIRCLE, 0xFF00FF00, 1.0f, ActionType.KEY_CODE, 96)
        val btnB = GamepadButton("btn_b", "B", 0.9f, 0.3f, 60f, ButtonShape.CIRCLE, 0xFFFF0000, 1.0f, ActionType.KEY_CODE, 97)

        // 1. Joystick active (pointer 1)
        engine.onJoystickDown(isLeft = true, pointerId = 1, centerX = 100f, centerY = 100f)

        // 2. Button A pressed (pointer 2)
        engine.onButtonDown(btnA, pointerId = 2, screenX = 800f, screenY = 400f)

        // 3. Button B pressed (pointer 3)
        engine.onButtonDown(btnB, pointerId = 3, screenX = 900f, screenY = 300f)

        // All 3 inputs active simultaneously
        assertEquals(3, engine.activePointerIds.value.size)
        assertTrue(engine.pressedButtons.value.contains("btn_a"))
        assertTrue(engine.pressedButtons.value.contains("btn_b"))

        // Release Button A only
        engine.onButtonUp(btnA, pointerId = 2)

        // Button B and Joystick MUST remain active!
        assertEquals(2, engine.activePointerIds.value.size)
        assertFalse(engine.pressedButtons.value.contains("btn_a"))
        assertTrue(engine.pressedButtons.value.contains("btn_b"))
        assertTrue(engine.liveLeftJoystick.value.isActive)

        // Release All cleans everything cleanly
        engine.releaseAll("Test cleanup")
        assertEquals(0, engine.activePointerIds.value.size)
        assertTrue(engine.pressedButtons.value.isEmpty())
        assertFalse(engine.liveLeftJoystick.value.isActive)
    }

    @Test
    fun testKeyboardModeJoystickWASD() {
        engine.setDriver(InputDriverType.SIMULATED)

        val keyboardJoy = JoystickConfig(
            id = "joy_wasd",
            label = "WASD",
            centerXRatio = 0.5f,
            centerYRatio = 0.5f,
            mode = JoystickMode.KEYBOARD,
            upKeyCode = KeyEvent.KEYCODE_W,
            upKeyLabel = "W",
            downKeyCode = KeyEvent.KEYCODE_S,
            downKeyLabel = "S",
            leftKeyCode = KeyEvent.KEYCODE_A,
            leftKeyLabel = "A",
            rightKeyCode = KeyEvent.KEYCODE_D,
            rightKeyLabel = "D"
        )

        engine.onJoystickDown(isLeft = true, pointerId = 1, centerX = 100f, centerY = 100f)

        // Push Up (W)
        engine.onJoystickMove(
            config = keyboardJoy,
            isLeft = true,
            pointerId = 1,
            deltaX = 0f,
            deltaY = -80f,
            maxRadius = 100f,
            screenCenterX = 100f,
            screenCenterY = 100f
        )
        assertTrue(engine.liveLeftJoystick.value.isActive)
        assertTrue(engine.liveLeftJoystick.value.normalizedY < -0.5f)

        // Push Up-Right (W + D simultaneously)
        engine.onJoystickMove(
            config = keyboardJoy,
            isLeft = true,
            pointerId = 1,
            deltaX = 70f,
            deltaY = -70f,
            maxRadius = 100f,
            screenCenterX = 100f,
            screenCenterY = 100f
        )
        assertTrue(engine.liveLeftJoystick.value.normalizedX > 0.4f)
        assertTrue(engine.liveLeftJoystick.value.normalizedY < -0.4f)

        // Release stick
        engine.onJoystickUp(isLeft = true, pointerId = 1)
        assertFalse(engine.liveLeftJoystick.value.isActive)
    }
}
