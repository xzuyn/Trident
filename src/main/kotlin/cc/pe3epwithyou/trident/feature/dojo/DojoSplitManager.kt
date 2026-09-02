package cc.pe3epwithyou.trident.feature.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.state.DojoCourseSplits
import cc.pe3epwithyou.trident.state.DojoSplit
import cc.pe3epwithyou.trident.state.PlayerStateIO
import cc.pe3epwithyou.trident.utils.Logger
import cc.pe3epwithyou.trident.utils.playerState

object DojoSplitManager {
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
