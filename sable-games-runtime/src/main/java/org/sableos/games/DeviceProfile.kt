package org.sableos.games

import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The phone the games run on, read once at start, so the keys fit its keyboard. The screen needs no
 * profile: [GameLayout] follows the window, so the Q25's 720 x 720 and the Titan 2's 1440 x 1440 both
 * get the square layout.
 *
 * - [Q25]: the trackpad gives arrows; Alt+W E R S D F Z X C are 1-9.
 * - [Titan2]: no arrows, trackpad or Menu key, so I J K L move (I up, J left, K down, L right);
 *   Alt+W E R S D F X C V are 1-9 and Alt+Q is 0.
 * - [P26]: the Zinwa P26, built on the BlackBerry Passport's square screen and three-row keyboard.
 *   It has no arrows or trackpad either, so I J K L move; Alt+W E R S D F Z X C are 1-9 as on the Q25.
 * - [KeyboardNoArrows]: another phone with a hardware keyboard and no arrows or trackpad; I J K L move.
 * - [Other]: a touch phone, or one whose arrows work; nothing changes.
 */
internal enum class DeviceProfile(
    /** I J K L move the cursor, because the phone has no arrow keys. */
    val letterMoves: Boolean,
    /** The keys that type 1-9 with Alt, used as plain digits where no letter has another meaning. */
    val letterDigits: Map<Int, Int>,
) {
    Q25(letterMoves = false, letterDigits = Digits.Q25),
    Titan2(letterMoves = true, letterDigits = Digits.TITAN2),
    P26(letterMoves = true, letterDigits = Digits.Q25),
    KeyboardNoArrows(letterMoves = true, letterDigits = Digits.Q25),
    Other(letterMoves = false, letterDigits = Digits.Q25),
    ;

    /** The name shown in Controls and How to play. */
    val label: String
        get() =
            when (this) {
                Q25 -> "Zinwa Q25"
                Titan2 -> "Unihertz Titan 2"
                P26 -> "Zinwa P26"
                KeyboardNoArrows -> "Keyboard phone"
                Other -> "This phone"
            }

    private object Digits {
        val Q25 =
            mapOf(
                KeyEvent.KEYCODE_W to 1,
                KeyEvent.KEYCODE_E to 2,
                KeyEvent.KEYCODE_R to 3,
                KeyEvent.KEYCODE_S to 4,
                KeyEvent.KEYCODE_D to 5,
                KeyEvent.KEYCODE_F to 6,
                KeyEvent.KEYCODE_Z to 7,
                KeyEvent.KEYCODE_X to 8,
                KeyEvent.KEYCODE_C to 9,
            )
        val TITAN2 =
            mapOf(
                KeyEvent.KEYCODE_W to 1,
                KeyEvent.KEYCODE_E to 2,
                KeyEvent.KEYCODE_R to 3,
                KeyEvent.KEYCODE_S to 4,
                KeyEvent.KEYCODE_D to 5,
                KeyEvent.KEYCODE_F to 6,
                KeyEvent.KEYCODE_X to 7,
                KeyEvent.KEYCODE_C to 8,
                KeyEvent.KEYCODE_V to 9,
            )
    }

    companion object {
        /** I up, J left, K down, L right, on a phone without arrows. */
        val LETTER_MOVES: Map<Int, GameCommand.Move> =
            mapOf(
                KeyEvent.KEYCODE_I to GameCommand.Move(0, -1),
                KeyEvent.KEYCODE_J to GameCommand.Move(-1, 0),
                KeyEvent.KEYCODE_K to GameCommand.Move(0, 1),
                KeyEvent.KEYCODE_L to GameCommand.Move(1, 0),
            )

        /**
         * Works the profile out from the build's names and the keyboard Android reports, without
         * Android, so it can be tested. The model names are matched loosely ("Titan 2", "TITAN2",
         * "Titan_2"), and an unknown keyboard phone falls back on what its configuration says.
         */
        fun detect(
            manufacturer: String,
            model: String,
            device: String,
            hasQwerty: Boolean,
            hasArrows: Boolean,
        ): DeviceProfile {
            val names = listOf(manufacturer + model, device).map { it.lowercase().filter(Char::isLetterOrDigit) }
            return when {
                names.any { "titan2" in it } -> Titan2
                names.any { "q25" in it } -> Q25
                names.any { "zinwap26" in it || it == "p26" || "passport" in it } -> P26
                hasQwerty && !hasArrows -> KeyboardNoArrows
                else -> Other
            }
        }

        /** The profile of the phone the app runs on. */
        fun current(configuration: Configuration): DeviceProfile =
            detect(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                model = Build.MODEL.orEmpty(),
                device = Build.DEVICE.orEmpty(),
                hasQwerty = configuration.keyboard == Configuration.KEYBOARD_QWERTY,
                hasArrows =
                    configuration.navigation == Configuration.NAVIGATION_DPAD ||
                        configuration.navigation == Configuration.NAVIGATION_TRACKBALL,
            )
    }
}

/** The profile of this phone; [MainActivity] provides it. */
internal val LocalDeviceProfile = staticCompositionLocalOf { DeviceProfile.Other }
