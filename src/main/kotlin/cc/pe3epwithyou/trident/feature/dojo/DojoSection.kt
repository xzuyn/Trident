package cc.pe3epwithyou.trident.feature.dojo

/**
 * Every Parkour Warrior: Dojo course has the same fixed shape: 9 mandatory main-path
 * obstacles ("M1-1".."M3-3"), 3 optional bonus branches of 3 obstacles each ("B1-1".."B3-3"),
 * and a choice of one of three endings, worth 1/2/3 medals — 21 medals maximum. Endings share
 * the bonus branches' "B<n>-<m>" naming but with branch number 4 (e.g. "B4-1" is the easy
 * ending), with the second number picking the difficulty.
 */
enum class DojoSection {
    MAIN, BONUS_1, BONUS_2, BONUS_3, ENDING_EASY, ENDING_MEDIUM, ENDING_HARD
}

/** Groups the [DojoSection] variants that should be visually separated in the split list. */
enum class DojoSectionGroup {
    MAIN, BONUS_1, BONUS_2, BONUS_3, ENDING
}

fun DojoSection.group(): DojoSectionGroup = when (this) {
    DojoSection.MAIN -> DojoSectionGroup.MAIN
    DojoSection.BONUS_1 -> DojoSectionGroup.BONUS_1
    DojoSection.BONUS_2 -> DojoSectionGroup.BONUS_2
    DojoSection.BONUS_3 -> DojoSectionGroup.BONUS_3
    DojoSection.ENDING_EASY, DojoSection.ENDING_MEDIUM, DojoSection.ENDING_HARD -> DojoSectionGroup.ENDING
}

private val BRANCH_PATTERN = Regex("""B(\d)-(\d)""")

/**
 * Maps a level's display name (e.g. `M1-1`, `B2-3`, `B4-1`) to its place in the course.
 * Not something pulled from an official source, just observed naming — if a level ever gets
 * misclassified it only affects whether its not-yet-reached placeholder row is hidden by the
 * route planner; levels you've actually reached or completed are always shown regardless.
 */
fun classifyDojoSection(levelName: String): DojoSection {
    val match = BRANCH_PATTERN.find(levelName.uppercase()) ?: return DojoSection.MAIN
    val branch = match.groupValues[1].toIntOrNull() ?: return DojoSection.MAIN
    val index = match.groupValues[2].toIntOrNull() ?: 3
    return when (branch) {
        1 -> DojoSection.BONUS_1
        2 -> DojoSection.BONUS_2
        3 -> DojoSection.BONUS_3
        4 -> when (index) {
            1 -> DojoSection.ENDING_EASY
            2 -> DojoSection.ENDING_MEDIUM
            else -> DojoSection.ENDING_HARD
        }

        else -> DojoSection.MAIN
    }
}

/**
 * Transitions worth showing as their own split rather than merging into the level that
 * follows: the choice points where a bonus branch could be taken instead of continuing on the
 * main path, the returns from each bonus branch back onto the main path, and the final choice
 * of ending. Every other transition is a short, fixed "connecting section" between two
 * obstacles that are always run back-to-back, and stays merged into the level's own row.
 */
val ALWAYS_SEPARATE_TRANSITIONS: Set<String> = setOf(
    //MAIN ROUTE
    "START_M1-1",
    "M1-3_M2-1",
    "M2-3_M3-1",
    "M3-3_B4-1",
    "M3-3_B4-2",
    "M3-3_B4-3",
    //BONUS 1
    "START_B1-1",
    "B1-3_M1-1",
    //BONUS 2
    "M1-3_B2-1",
    "B2-3_M2-1",
    //BONUS 3
    "M2-3_B3-1",
    "B3-3_M3-1",
)
