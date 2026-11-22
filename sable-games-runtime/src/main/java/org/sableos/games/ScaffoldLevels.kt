package org.sableos.games

/**
 * Sable Scaffold: a single-screen climbing game. The climber, Pip, goes up a building site and dodges what
 * Clunk, a site robot stuck on "throw", sends down, to rescue the engineer Juno at the top. Four levels.
 *
 * The field is 720 x 672 units, (0, 0) at the top left; y grows downwards. A [Surface]'s y is its walking
 * line. Everything here is plain data so the levels can be checked on the JVM ([ScaffoldLevelsTest]).
 */
internal object ScaffoldField {
    const val WIDTH = 720f
    const val HEIGHT = 672f

    /** Height of the status bar drawn above the field, so the whole board is a 720 x 720 square. */
    const val HUD = 48f
    const val GROUND_Y = 628f
}

/** What Clunk throws. [width] and [height] are the hit box, in field units. */
internal enum class HazardKind(
    val width: Float,
    val height: Float,
) {
    Pipe(22f, 22f),
    Cart(36f, 20f),
    Spool(24f, 24f),
    Brick(20f, 11f),
}

/**
 * Somewhere to stand, from [x0] to [x1] at height [y]. [flow] is the way things roll along it (-1 left,
 * +1 right, 0 = whichever way they were thrown). A [solid] block (a stair) can't be walked into from
 * the side when it is higher than your feet.
 */
internal data class Surface(
    val x0: Float,
    val x1: Float,
    val y: Float,
    val flow: Int = 0,
    val solid: Boolean = false,
)

/** A ladder at [x] from the surface at [top] down to the one at [bottom]. */
internal data class Ladder(
    val x: Float,
    val top: Float,
    val bottom: Float,
)

/** A hoist: a platform that visits each of [stops] (platform heights) in turn and back, waiting at each. */
internal data class Lift(
    val x0: Float,
    val x1: Float,
    val stops: List<Float>,
    val speed: Float = 70f,
    val wait: Float = 1f,
)

internal data class Spot(
    val x: Float,
    val y: Float,
)

/** One throw: [kind] from [x] ([y] above the field's walking lines), rolling [dir] (0 = along the surface it lands on). */
internal data class ThrowSpec(
    val kind: HazardKind,
    val x: Float,
    val dir: Int = 0,
    val y: Float = 100f,
)

/** A wrecking ball hanging from [pivot] on a cable [length] long, swinging up to [amplitudeDeg] each side. */
internal data class Pendulum(
    val pivot: Spot,
    val length: Float,
    val amplitudeDeg: Float,
    val periodSeconds: Float,
    val radius: Float = 16f,
)

internal enum class BuildStyle { Timber, Concrete }

internal data class ScaffoldLevel(
    val number: Int,
    val name: String,
    val style: BuildStyle,
    val surfaces: List<Surface>,
    val ladders: List<Ladder>,
    val lifts: List<Lift>,
    val bolts: List<Spot>,
    val hats: List<Spot>,
    val heroStart: Spot,
    val juno: Spot,
    val clunk: Spot,
    val clunkFlip: Boolean,
    val throws: List<ThrowSpec>,
    val throwInterval: Float,
    val rollSpeed: Float,
    val bonus: Int,
    val pendulum: Pendulum? = null,
) {
    /** How many bolts the level holds, for "8/8" in the level list. */
    val boltCount: Int get() = bolts.size
}

internal object ScaffoldLevels {
    const val COUNT = 4

    private const val FLOOR_2 = 500f
    private const val FLOOR_3 = 372f
    private const val FLOOR_4 = 244f
    private const val TOP = 116f

    /** Groundworks: pipes roll down four decks that sit against alternate walls. */
    private val level1 =
        ScaffoldLevel(
            number = 1,
            name = "Groundworks",
            style = BuildStyle.Timber,
            surfaces =
                listOf(
                    Surface(0f, 720f, ScaffoldField.GROUND_Y, flow = 1),
                    Surface(70f, 720f, FLOOR_2, flow = -1),
                    Surface(0f, 650f, FLOOR_3, flow = 1),
                    Surface(70f, 720f, FLOOR_4, flow = -1),
                    Surface(190f, 520f, TOP),
                ),
            ladders =
                listOf(
                    Ladder(560f, FLOOR_2, ScaffoldField.GROUND_Y),
                    Ladder(130f, FLOOR_3, FLOOR_2),
                    Ladder(540f, FLOOR_4, FLOOR_3),
                    Ladder(300f, TOP, FLOOR_4),
                ),
            lifts = emptyList(),
            bolts =
                listOf(
                    Spot(250f, 480f),
                    Spot(290f, 480f),
                    Spot(400f, 352f),
                    Spot(440f, 352f),
                    Spot(200f, 224f),
                    Spot(240f, 224f),
                    Spot(610f, 224f),
                    Spot(400f, 96f),
                ),
            hats = listOf(Spot(300f, FLOOR_3)),
            heroStart = Spot(470f, ScaffoldField.GROUND_Y),
            juno = Spot(470f, TOP),
            clunk = Spot(240f, TOP),
            clunkFlip = true,
            throws =
                listOf(
                    ThrowSpec(HazardKind.Pipe, 185f, dir = -1),
                    ThrowSpec(HazardKind.Pipe, 185f, dir = 1),
                ),
            throwInterval = 3.0f,
            rollSpeed = 80f,
            bonus = 4000,
        )

    /** Scaffold Tower: wheelbarrows, split decks and a hoist shaft up the middle. */
    private val level2 =
        ScaffoldLevel(
            number = 2,
            name = "Scaffold Tower",
            style = BuildStyle.Timber,
            surfaces =
                listOf(
                    Surface(0f, 720f, ScaffoldField.GROUND_Y, flow = -1),
                    Surface(40f, 300f, FLOOR_2, flow = -1),
                    Surface(420f, 700f, FLOOR_2, flow = -1),
                    Surface(0f, 260f, FLOOR_3, flow = 1),
                    Surface(360f, 720f, FLOOR_3, flow = -1),
                    Surface(120f, 600f, FLOOR_4, flow = 1),
                    Surface(220f, 500f, TOP),
                ),
            ladders =
                listOf(
                    Ladder(100f, FLOOR_2, ScaffoldField.GROUND_Y),
                    Ladder(610f, FLOOR_2, ScaffoldField.GROUND_Y),
                    Ladder(220f, FLOOR_3, FLOOR_2),
                    Ladder(640f, FLOOR_3, FLOOR_2),
                    Ladder(150f, FLOOR_4, FLOOR_3),
                    Ladder(520f, FLOOR_4, FLOOR_3),
                    Ladder(420f, TOP, FLOOR_4),
                ),
            lifts = listOf(Lift(300f, 360f, listOf(ScaffoldField.GROUND_Y, FLOOR_2, FLOOR_3, FLOOR_4))),
            bolts =
                listOf(
                    Spot(80f, 480f),
                    Spot(120f, 480f),
                    Spot(480f, 480f),
                    Spot(520f, 480f),
                    Spot(60f, 352f),
                    Spot(420f, 352f),
                    Spot(680f, 352f),
                    Spot(200f, 224f),
                    Spot(470f, 224f),
                    Spot(340f, 96f),
                ),
            hats = listOf(Spot(260f, FLOOR_4)),
            heroStart = Spot(200f, ScaffoldField.GROUND_Y),
            juno = Spot(270f, TOP),
            clunk = Spot(470f, TOP),
            clunkFlip = false,
            throws =
                listOf(
                    ThrowSpec(HazardKind.Cart, 510f, dir = 1),
                    ThrowSpec(HazardKind.Cart, 510f, dir = -1),
                ),
            throwInterval = 2.4f,
            rollSpeed = 110f,
            bonus = 5000,
        )

    /**
     * Concrete Frame: no ladders. Each floor has a two-step stair that rises to the right, with a hole in the
     * floor above it, so the climb runs from the left of the frame up to the right and a lift takes you to
     * Juno. Spools fall down the same stairs in a fixed pattern.
     */
    private val level3 =
        ScaffoldLevel(
            number = 3,
            name = "Concrete Frame",
            style = BuildStyle.Concrete,
            surfaces =
                listOf(
                    Surface(0f, 720f, ScaffoldField.GROUND_Y, flow = -1),
                    Surface(100f, 140f, 585f, flow = -1, solid = true),
                    Surface(140f, 180f, 542f, flow = -1, solid = true),
                    Surface(0f, 100f, FLOOR_2, flow = -1),
                    Surface(180f, 720f, FLOOR_2, flow = -1),
                    Surface(300f, 340f, 457f, flow = -1, solid = true),
                    Surface(340f, 380f, 414f, flow = -1, solid = true),
                    Surface(0f, 300f, FLOOR_3, flow = -1),
                    Surface(380f, 720f, FLOOR_3, flow = -1),
                    Surface(500f, 540f, 329f, flow = -1, solid = true),
                    Surface(540f, 580f, 286f, flow = -1, solid = true),
                    Surface(0f, 500f, FLOOR_4, flow = 1),
                    Surface(580f, 720f, FLOOR_4, flow = -1),
                    Surface(0f, 340f, TOP),
                    Surface(590f, 720f, TOP),
                ),
            ladders = emptyList(),
            lifts = listOf(Lift(620f, 700f, listOf(FLOOR_4, TOP), speed = 60f, wait = 1.5f)),
            bolts =
                listOf(
                    Spot(60f, 608f),
                    Spot(230f, 480f),
                    Spot(270f, 480f),
                    Spot(450f, 480f),
                    Spot(500f, 480f),
                    Spot(360f, 394f),
                    Spot(420f, 352f),
                    Spot(470f, 352f),
                    Spot(660f, 352f),
                    Spot(560f, 266f),
                    Spot(610f, 224f),
                    Spot(660f, 224f),
                ),
            hats = listOf(Spot(400f, FLOOR_2)),
            heroStart = Spot(40f, ScaffoldField.GROUND_Y),
            juno = Spot(670f, TOP),
            clunk = Spot(80f, TOP),
            clunkFlip = false,
            throws = listOf(ThrowSpec(HazardKind.Spool, 350f, dir = 1)),
            throwInterval = 2.2f,
            rollSpeed = 100f,
            bonus = 6000,
        )

    /** Crane Top: everything at once, a wrecking ball, and Clunk standing between you and Juno. */
    private val level4 =
        ScaffoldLevel(
            number = 4,
            name = "Crane Top",
            style = BuildStyle.Timber,
            surfaces =
                listOf(
                    Surface(0f, 720f, ScaffoldField.GROUND_Y, flow = 1),
                    Surface(60f, 400f, FLOOR_2, flow = -1),
                    Surface(320f, 720f, FLOOR_3, flow = -1),
                    Surface(200f, 700f, FLOOR_4),
                    Surface(60f, 400f, TOP),
                ),
            ladders =
                listOf(
                    Ladder(140f, FLOOR_2, ScaffoldField.GROUND_Y),
                    Ladder(360f, FLOOR_3, FLOOR_2),
                    Ladder(620f, FLOOR_4, FLOOR_3),
                    Ladder(240f, TOP, FLOOR_4),
                ),
            lifts = emptyList(),
            bolts =
                listOf(
                    Spot(100f, 480f),
                    Spot(200f, 480f),
                    Spot(300f, 480f),
                    Spot(540f, 352f),
                    Spot(580f, 352f),
                    Spot(660f, 352f),
                    Spot(260f, 224f),
                    Spot(320f, 224f),
                    Spot(500f, 224f),
                    Spot(660f, 224f),
                    Spot(160f, 96f),
                    Spot(230f, 96f),
                    Spot(300f, 96f),
                    Spot(300f, 608f),
                ),
            hats = listOf(Spot(420f, FLOOR_3)),
            heroStart = Spot(60f, ScaffoldField.GROUND_Y),
            juno = Spot(110f, TOP),
            clunk = Spot(350f, TOP),
            clunkFlip = false,
            throws =
                listOf(
                    ThrowSpec(HazardKind.Pipe, 410f, dir = -1),
                    ThrowSpec(HazardKind.Brick, 450f, y = 70f),
                    ThrowSpec(HazardKind.Spool, 410f, dir = 1),
                    ThrowSpec(HazardKind.Cart, 410f, dir = -1),
                    ThrowSpec(HazardKind.Brick, 520f, y = 70f),
                    ThrowSpec(HazardKind.Pipe, 410f, dir = 1),
                ),
            throwInterval = 2.0f,
            rollSpeed = 120f,
            bonus = 8000,
            pendulum = Pendulum(Spot(572f, 78f), length = 150f, amplitudeDeg = 28f, periodSeconds = 2.4f),
        )

    val all: List<ScaffoldLevel> = listOf(level1, level2, level3, level4)

    fun level(number: Int): ScaffoldLevel = all[(number - 1).coerceIn(0, COUNT - 1)]
}
