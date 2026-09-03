package cc.pe3epwithyou.trident.feature.dojo

/**
 * Every Parkour Warrior: Dojo course has the same fixed shape: 9 mandatory main-path
 * obstacles, 3 optional bonus branches of 3 obstacles each, and a choice of one of three
 * endings (worth 1/2/3 medals) — 21 medals maximum.
 */
enum class DojoSection {
    MAIN, BONUS_1, BONUS_2, BONUS_3, ENDING_EASY, ENDING_MEDIUM, ENDING_HARD
}

/**
 * Guesses which part of the course a level belongs to, from its display name (e.g. `M1-1`,
 * `B2-3`). This is a best-effort heuristic based on common naming conventions, not something
 * pulled from an official source — if a level gets misclassified it only affects whether its
 * not-yet-reached placeholder row is hidden by the route planner; levels you've actually
 * reached or completed are always shown regardless.
 */
fun classifyDojoSection(levelName: String): DojoSection {
    val n = levelName.uppercase()
    return when {
        n.startsWith("B1") -> DojoSection.BONUS_1
        n.startsWith("B2") -> DojoSection.BONUS_2
        n.startsWith("B3") -> DojoSection.BONUS_3
        "END" in n || n.startsWith("E") -> when {
            "EASY" in n -> DojoSection.ENDING_EASY
            "MED" in n -> DojoSection.ENDING_MEDIUM
            "HARD" in n -> DojoSection.ENDING_HARD
            n.endsWith("1") -> DojoSection.ENDING_EASY
            n.endsWith("2") -> DojoSection.ENDING_MEDIUM
            else -> DojoSection.ENDING_HARD
        }

        else -> DojoSection.MAIN
    }
}
