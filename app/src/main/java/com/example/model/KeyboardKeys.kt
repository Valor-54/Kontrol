package com.example.model

import android.view.KeyEvent

data class KeyDefinition(
    val code: Int,
    val label: String,
    val category: String,
    val description: String = ""
)

object KeyboardKeys {

    val MOVEMENT_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_W, "W", "WASD & Hareket", "İleri / Yukarı"),
        KeyDefinition(KeyEvent.KEYCODE_A, "A", "WASD & Hareket", "Sol"),
        KeyDefinition(KeyEvent.KEYCODE_S, "S", "WASD & Hareket", "Geri / Aşağı"),
        KeyDefinition(KeyEvent.KEYCODE_D, "D", "WASD & Hareket", "Sağ"),
        KeyDefinition(KeyEvent.KEYCODE_DPAD_UP, "YUKARI", "WASD & Hareket", "Ok Tuşu Yukarı"),
        KeyDefinition(KeyEvent.KEYCODE_DPAD_DOWN, "AŞAĞI", "WASD & Hareket", "Ok Tuşu Aşağı"),
        KeyDefinition(KeyEvent.KEYCODE_DPAD_LEFT, "SOL", "WASD & Hareket", "Ok Tuşu Sol"),
        KeyDefinition(KeyEvent.KEYCODE_DPAD_RIGHT, "SAĞ", "WASD & Hareket", "Ok Tuşu Sağ"),
        KeyDefinition(KeyEvent.KEYCODE_SPACE, "SPACE", "WASD & Hareket", "Boşluk / Zıpla"),
        KeyDefinition(KeyEvent.KEYCODE_SHIFT_LEFT, "SHIFT (L)", "WASD & Hareket", "Sol Shift / Koş"),
        KeyDefinition(KeyEvent.KEYCODE_SHIFT_RIGHT, "SHIFT (R)", "WASD & Hareket", "Sağ Shift"),
        KeyDefinition(KeyEvent.KEYCODE_CTRL_LEFT, "CTRL (L)", "WASD & Hareket", "Sol Control / Çömel"),
        KeyDefinition(KeyEvent.KEYCODE_CTRL_RIGHT, "CTRL (R)", "WASD & Hareket", "Sağ Control"),
        KeyDefinition(KeyEvent.KEYCODE_ALT_LEFT, "ALT (L)", "WASD & Hareket", "Sol Alt"),
        KeyDefinition(KeyEvent.KEYCODE_ALT_RIGHT, "ALT (R)", "WASD & Hareket", "Sağ Alt")
    )

    val LETTER_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_A, "A", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_B, "B", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_C, "C", "Harfler (A-Z)", "Çömel / Aksiyon"),
        KeyDefinition(KeyEvent.KEYCODE_D, "D", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_E, "E", "Harfler (A-Z)", "Etkileşim / Aç"),
        KeyDefinition(KeyEvent.KEYCODE_F, "F", "Harfler (A-Z)", "Kullan / Fener"),
        KeyDefinition(KeyEvent.KEYCODE_G, "G", "Harfler (A-Z)", "Bomba / Ekipman"),
        KeyDefinition(KeyEvent.KEYCODE_H, "H", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_I, "I", "Harfler (A-Z)", "Envanter"),
        KeyDefinition(KeyEvent.KEYCODE_J, "J", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_K, "K", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_L, "L", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_M, "M", "Harfler (A-Z)", "Harita"),
        KeyDefinition(KeyEvent.KEYCODE_N, "N", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_O, "O", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_P, "P", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_Q, "Q", "Harfler (A-Z)", "Hızlı Değiştir / Sola Eğil"),
        KeyDefinition(KeyEvent.KEYCODE_R, "R", "Harfler (A-Z)", "Şarjör Değiştir"),
        KeyDefinition(KeyEvent.KEYCODE_S, "S", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_T, "T", "Harfler (A-Z)", "Sohbet"),
        KeyDefinition(KeyEvent.KEYCODE_U, "U", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_V, "V", "Harfler (A-Z)", "Sesli Sohbet / Yakın"),
        KeyDefinition(KeyEvent.KEYCODE_W, "W", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_X, "X", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_Y, "Y", "Harfler (A-Z)"),
        KeyDefinition(KeyEvent.KEYCODE_Z, "Z", "Harfler (A-Z)", "Yat / Yavaş")
    )

    val NUMBER_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_1, "1", "Rakamlar (0-9)", "Silah 1"),
        KeyDefinition(KeyEvent.KEYCODE_2, "2", "Rakamlar (0-9)", "Silah 2"),
        KeyDefinition(KeyEvent.KEYCODE_3, "3", "Rakamlar (0-9)", "Tabanca / Bıçak"),
        KeyDefinition(KeyEvent.KEYCODE_4, "4", "Rakamlar (0-9)", "Bomba / Ekipman"),
        KeyDefinition(KeyEvent.KEYCODE_5, "5", "Rakamlar (0-9)", "Kit"),
        KeyDefinition(KeyEvent.KEYCODE_6, "6", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_7, "7", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_8, "8", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_9, "9", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_0, "0", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_1, "NUM 1", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_2, "NUM 2", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_3, "NUM 3", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_4, "NUM 4", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_5, "NUM 5", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_6, "NUM 6", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_7, "NUM 7", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_8, "NUM 8", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_9, "NUM 9", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_0, "NUM 0", "Rakamlar (0-9)"),
        KeyDefinition(KeyEvent.KEYCODE_NUMPAD_ENTER, "NUM ENTER", "Rakamlar (0-9)")
    )

    val SPECIAL_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_ENTER, "ENTER", "Özel & Kontrol", "Giriş / Onayla"),
        KeyDefinition(KeyEvent.KEYCODE_ESCAPE, "ESC", "Özel & Kontrol", "Menü / Çıkış"),
        KeyDefinition(KeyEvent.KEYCODE_TAB, "TAB", "Özel & Kontrol", "Skor Tablosu / Envanter"),
        KeyDefinition(KeyEvent.KEYCODE_DEL, "BACKSPACE", "Özel & Kontrol", "Geri Sil"),
        KeyDefinition(KeyEvent.KEYCODE_FORWARD_DEL, "DELETE", "Özel & Kontrol", "İleri Sil"),
        KeyDefinition(KeyEvent.KEYCODE_CAPS_LOCK, "CAPS LOCK", "Özel & Kontrol"),
        KeyDefinition(KeyEvent.KEYCODE_PAGE_UP, "PG UP", "Özel & Kontrol"),
        KeyDefinition(KeyEvent.KEYCODE_PAGE_DOWN, "PG DOWN", "Özel & Kontrol"),
        KeyDefinition(KeyEvent.KEYCODE_MOVE_HOME, "HOME", "Özel & Kontrol"),
        KeyDefinition(KeyEvent.KEYCODE_MOVE_END, "END", "Özel & Kontrol"),
        KeyDefinition(KeyEvent.KEYCODE_INSERT, "INSERT", "Özel & Kontrol")
    )

    val FUNCTION_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_F1, "F1", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F2, "F2", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F3, "F3", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F4, "F4", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F5, "F5", "Fonksiyon (F1-F12)", "Hızlı Kayıt / Yenile"),
        KeyDefinition(KeyEvent.KEYCODE_F6, "F6", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F7, "F7", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F8, "F8", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F9, "F9", "Fonksiyon (F1-F12)", "Hızlı Yükle"),
        KeyDefinition(KeyEvent.KEYCODE_F10, "F10", "Fonksiyon (F1-F12)"),
        KeyDefinition(KeyEvent.KEYCODE_F11, "F11", "Fonksiyon (F1-F12)", "Tam Ekran"),
        KeyDefinition(KeyEvent.KEYCODE_F12, "F12", "Fonksiyon (F1-F12)", "Ekran Görüntüsü")
    )

    val SYMBOL_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_MINUS, "-", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_EQUALS, "=", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_LEFT_BRACKET, "[", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_RIGHT_BRACKET, "]", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_SEMICOLON, ";", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_APOSTROPHE, "'", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_COMMA, ",", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_PERIOD, ".", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_SLASH, "/", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_BACKSLASH, "\\", "Semboller"),
        KeyDefinition(KeyEvent.KEYCODE_GRAVE, "`", "Semboller", "Konsol / Tilde")
    )

    val GAMEPAD_KEYS = listOf(
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_A, "BTN_A", "Gamepad", "A / Çapraz"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_B, "BTN_B", "Gamepad", "B / Daire"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_X, "BTN_X", "Gamepad", "X / Kare"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_Y, "BTN_Y", "Gamepad", "Y / Üçgen"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_L1, "LB / L1", "Gamepad", "Sol Üst Tetik"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_R1, "RB / R1", "Gamepad", "Sağ Üst Tetik"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_L2, "LT / L2", "Gamepad", "Sol Alt Tetik"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_R2, "RT / R2", "Gamepad", "Sağ Alt Tetik"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_THUMBL, "L3", "Gamepad", "Sol Çubuk Basma"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_THUMBR, "R3", "Gamepad", "Sağ Çubuk Basma"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_START, "START", "Gamepad", "Başlat / Menü"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_SELECT, "SELECT", "Gamepad", "Seç / Harita"),
        KeyDefinition(KeyEvent.KEYCODE_BUTTON_MODE, "MODE", "Gamepad", "Kılavuz / Home")
    )

    val ALL_KEYS: List<KeyDefinition> =
        MOVEMENT_KEYS + LETTER_KEYS + NUMBER_KEYS + SPECIAL_KEYS + FUNCTION_KEYS + SYMBOL_KEYS + GAMEPAD_KEYS

    val CATEGORIES = listOf(
        "WASD & Hareket",
        "Harfler (A-Z)",
        "Rakamlar (0-9)",
        "Özel & Kontrol",
        "Fonksiyon (F1-F12)",
        "Semboller",
        "Gamepad"
    )

    fun findLabel(code: Int): String {
        return ALL_KEYS.firstOrNull { it.code == code }?.label ?: "KEY_$code"
    }
}
