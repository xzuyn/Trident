package cc.pe3epwithyou.trident.feature.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.interfaces.DialogCollection
import cc.pe3epwithyou.trident.state.DojoCourseSplits
import cc.pe3epwithyou.trident.state.DojoSplit
import cc.pe3epwithyou.trident.state.PlayerStateIO
import cc.pe3epwithyou.trident.utils.Logger
import cc.pe3epwithyou.trident.utils.ScoreboardUtils
import cc.pe3epwithyou.trident.utils.playerState

object DojoSplitManager {
    private val COURSE_NAME_PATTERN = Regex("""COURSE: (.*)""")

    /** The most recently seen course this session, so the splits dialog has something to show before/between runs. */
    var lastCourseName: String? = null
        private set

    /**
     * Polls the sidebar scoreboard for the currently selected course, so the splits dialog
     * updates as soon as you walk into a course rather than waiting for a run to start.
     * Called once per tick while in Parkour Warrior: Dojo (see [cc.pe3epwithyou.trident.Trident]).
     */
    fun pollCourseName() {
        if (DojoSplitTimer.instance != null) return // an active run already knows its own course
        val detected = ScoreboardUtils.findInScoreboard(COURSE_NAME_PATTERN)?.groupValues?.getOrNull(1) ?: return
        if (detected == lastCourseName) return
        lastCourseName = detected
        DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
    }

    /**
     * Gets (or creates) the [DojoCourseSplits] for a course, keyed off its display name.
     *
     * Daily challenge courses are collapsed into a single "daily" bucket, since their
     * course name changes every day but the levels are the same layout.
     */
    fun getCourseSplits(courseName: String): DojoCourseSplits {
        val name = if (courseName.lowercase().contains("daily challenge")) "daily" else courseName
        return playerState().dojoSplits.getOrPut(name) {
            Logger.debugLog("DojoSplitManager - Created splits for: $name")
            DojoCourseSplits()
        }
    }

    fun saveSplit(courseName: String, levelUid: String, levelName: String, timeMillis: Long) {
        val splits = getCourseSplits(courseName)
        splits.levels[levelUid] = splits.levels[levelUid]?.addTime(timeMillis) ?: DojoSplit(timeMillis, timeMillis.toDouble())
        splits.levelNames[levelUid] = levelName
        Logger.debugLog("DojoSplitManager - Time (${timeMillis}ms) was saved with uid: $levelUid")
        PlayerStateIO.save()
    }

    /**
     * Returns the split time to compare against, in seconds, based on the configured save mode.
     */
    fun getSplitSeconds(courseName: String, levelUid: String): Double? {
        val split = getCourseSplits(courseName).levels[levelUid] ?: return null
        val millis = when (Config.Dojo.saveMode) {
            DojoSplitType.BEST -> split.best.toDouble()
            DojoSplitType.AVG -> split.avg
        }
        return millis / 1000.0
    }

    fun clearSplits() {
        playerState().dojoSplits.clear()
        PlayerStateIO.save()
    }
}
