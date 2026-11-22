package org.sableos.games

import kotlin.random.Random

/** How one guessed letter scored: in the right place, in the word elsewhere, or not in it. */
internal enum class LetterMark { Right, Elsewhere, Absent }

/**
 * Sable Words: find the hidden five-letter word in six guesses, typed on the keyboard. After each
 * guess every letter is marked [LetterMark.Right], [LetterMark.Elsewhere] or [LetterMark.Absent];
 * a repeated letter is marked Elsewhere only as many times as the answer still has it. Any five
 * letters may be guessed (there is no dictionary on the phone to check against).
 */
internal data class WordsGame(
    val answer: String,
    val guesses: List<String> = emptyList(),
    val typing: String = "",
) {
    val won: Boolean get() = guesses.lastOrNull() == answer
    val over: Boolean get() = won || guesses.size >= MAX_GUESSES

    fun type(letter: Char): WordsGame =
        if (over ||
            typing.length >= LENGTH
        ) {
            this
        } else {
            copy(typing = typing + letter.uppercaseChar())
        }

    fun erase(): WordsGame = if (over) this else copy(typing = typing.dropLast(1))

    /** Enter: the typed word becomes a guess once it has five letters. */
    fun submit(): WordsGame =
        if (over ||
            typing.length < LENGTH
        ) {
            this
        } else {
            copy(guesses = guesses + typing, typing = "")
        }

    /** The best mark each letter has earned so far, for the keyboard row under the board. */
    fun letterMarks(): Map<Char, LetterMark> {
        val best = mutableMapOf<Char, LetterMark>()
        guesses.forEach { guess ->
            guess.toList().zip(marks(guess, answer)).forEach { (letter, mark) ->
                val old = best[letter]
                if (old == null || mark.ordinal < old.ordinal) best[letter] = mark
            }
        }
        return best
    }

    companion object {
        const val LENGTH = 5
        const val MAX_GUESSES = 6

        fun marks(
            guess: String,
            answer: String,
        ): List<LetterMark> {
            val result = MutableList(guess.length) { LetterMark.Absent }
            val unmatched = mutableMapOf<Char, Int>()
            guess.indices.forEach { i ->
                if (guess[i] == answer[i]) {
                    result[i] = LetterMark.Right
                } else {
                    unmatched[answer[i]] = (unmatched[answer[i]] ?: 0) + 1
                }
            }
            guess.indices.filter { result[it] != LetterMark.Right }.forEach { i ->
                val left = unmatched[guess[i]] ?: 0
                if (left > 0) {
                    result[i] = LetterMark.Elsewhere
                    unmatched[guess[i]] = left - 1
                }
            }
            return result
        }

        fun start(random: Random): WordsGame = WordsGame(ANSWERS[random.nextInt(ANSWERS.size)])

        /** Common five-letter words, written for Sable. */
        val ANSWERS: List<String> =
            (
                "ABOUT ABOVE ACTOR ADMIT ADOPT AFTER AGAIN AGENT AGREE AHEAD ALARM ALBUM ALERT ALIKE ALIVE " +
                    "ALLOW ALONE ALONG ALTER AMONG ANGER ANGLE ANGRY APART APPLE APPLY ARENA ARGUE ARISE ARMOR " +
                    "ASIDE AWARD AWARE BADGE BAKER BASIC BEACH BEGIN BEING BELOW BENCH BIRTH BLACK BLADE BLAME " +
                    "BLANK BLEND BLIND BLOCK BLOOM BOARD BOOST BRAIN BRAND BRAVE BREAD BREAK BRICK BRIEF BRING " +
                    "BROAD BROWN BRUSH BUILD BUNCH CABIN CABLE CANDY CARRY CATCH CAUSE CHAIN CHAIR CHALK CHARM " +
                    "CHART CHASE CHEAP CHECK CHEST CHIEF CHILD CHORD CIVIC CLAIM CLASS CLEAN CLEAR CLERK CLICK " +
                    "CLIMB CLOCK CLOSE CLOUD COACH COAST COUNT COURT COVER CRAFT CRANE CREAM CRISP CROSS CROWD " +
                    "CROWN CURVE CYCLE DAILY DANCE DEPTH DIARY DOUBT DRAFT DRAMA DREAM DRESS DRIFT DRINK DRIVE " +
                    "EAGER EARLY EARTH EIGHT ELBOW EMPTY ENJOY ENTER ENTRY EQUAL ERROR EVENT EXACT EXIST EXTRA " +
                    "FAITH FALSE FANCY FAULT FEAST FENCE FIELD FIFTY FIGHT FINAL FIRST FLAME FLASH FLEET FLOAT " +
                    "FLOOR FLOUR FOCUS FORCE FRAME FRESH FRONT FROST FRUIT FUNNY GIANT GLASS GLOBE GLORY GRACE " +
                    "GRADE GRAIN GRAND GRANT GRAPE GRASS GREAT GREEN GROUP GUARD GUESS GUEST GUIDE HABIT HAPPY " +
                    "HARSH HEART HEAVY HONEY HORSE HOTEL HOUSE HUMAN HUMOR IDEAL IMAGE INDEX INNER INPUT ISSUE " +
                    "JEWEL JOINT JUDGE JUICE KNIFE KNOCK LABEL LARGE LASER LATER LAUGH LAYER LEARN LEAST LEAVE " +
                    "LEGAL LEMON LEVEL LIGHT LIMIT LOCAL LOGIC LOOSE LUCKY LUNCH MAGIC MAJOR MAKER MARCH MATCH " +
                    "MAYBE MEDIA MERCY METAL MIGHT MINOR MODEL MONEY MONTH MORAL MOTOR MOUNT MOUSE MOUTH MOVIE " +
                    "MUSIC NERVE NEVER NIGHT NOISE NORTH NOVEL NURSE OCEAN OFFER OFTEN OLIVE ORDER OTHER OUTER " +
                    "OWNER PAINT PANEL PAPER PARTY PEACE PEARL PHASE PHONE PHOTO PIANO PIECE PILOT PITCH PLACE " +
                    "PLAIN PLANE PLANT PLATE POINT POWER PRESS PRICE PRIDE PRIME PRINT PRIZE PROOF PROUD QUEEN " +
                    "QUICK QUIET QUOTE RADIO RAISE RANGE RAPID REACH READY RIVER ROBOT ROUGH ROUND ROUTE ROYAL " +
                    "SALAD SCALE SCENE SCOPE SCORE SENSE SERVE SEVEN SHADE SHAPE SHARE SHARP SHEEP SHELF SHELL " +
                    "SHIFT SHINE SHIRT SHOCK SHORE SHORT SIGHT SKILL SLEEP SLICE SLIDE SMALL SMART SMILE SMOKE " +
                    "SOLID SOLVE SOUND SOUTH SPACE SPARE SPEAK SPEED SPEND SPICE SPLIT SPORT STAFF STAGE STAIR " +
                    "STAND START STATE STEAM STEEL STICK STILL STONE STORE STORM STORY STOVE STUDY STYLE SUGAR " +
                    "SUNNY SWEET SWIFT TABLE TASTE TEACH THANK THEME THICK THING THINK THREE THROW TIGER TIGHT " +
                    "TITLE TODAY TOOTH TOPIC TORCH TOTAL TOUCH TOWER TRACK TRADE TRAIL TRAIN TREAT TREND TRIAL " +
                    "TRUCK TRUST TRUTH UNCLE UNDER UNION UNITY UPPER URBAN USUAL VALUE VIDEO VISIT VITAL VOICE " +
                    "WASTE WATCH WATER WHEEL WHITE WHOLE WOMAN WORLD WORRY WORTH WRITE YOUNG YOUTH ZEBRA"
            ).split(' ')
    }
}
