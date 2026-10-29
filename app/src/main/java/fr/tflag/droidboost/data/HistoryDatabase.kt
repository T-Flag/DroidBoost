package fr.tflag.droidboost.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One value of a time series; [metric] names come from `Metrics`, [tag] distinguishes sub-series. */
@Entity(tableName = "sample", indices = [Index(value = ["metric", "tag", "ts"])])
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val metric: String,
    val tag: String = "",
    val value: Double,
)

data class SeriesPoint(val bucket: Long, val value: Double)

@Entity(tableName = "charge_session", indices = [Index(value = ["startTs"])])
data class ChargeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    /** Null while the session is in progress. */
    val endTs: Long?,
    val lastTs: Long,
    val startLevel: Int,
    val endLevel: Int,
    val addedMah: Double?,
    val estimatedCapacityMah: Double?,
    val averagePowerW: Double?,
    val maxPowerW: Double,
    val maxTemperatureC: Float,
)

@Entity(tableName = "thermal_episode", indices = [Index(value = ["startTs"])])
data class ThermalEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    val endTs: Long?,
    val lastTs: Long,
    /** `ThermalStatus.level`. */
    val maxStatus: Int,
    val maxHeadroom: Float?,
    val foregroundPackage: String?,
    val charging: Boolean,
)

@Entity(tableName = "journal_event", indices = [Index(value = ["ts"]), Index(value = ["type", "ts"])])
data class JournalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    /** One of `JournalTypes`. */
    val type: String,
    val packageName: String? = null,
    val title: String,
    val details: String? = null,
    /** Finer category, e.g. the `ExitKind` of an EXIT event. */
    val subtype: String? = null,
)

/** Last known state of each package, diffed against the next snapshot to produce journal events. */
@Entity(tableName = "package_snapshot")
data class PackageSnapshotEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val lastUpdateTime: Long,
    val enabled: Boolean,
    val installedForUser: Boolean,
    /** Granted runtime permissions, one per line. */
    val grantedPermissions: String,
)

data class PackageCount(val packageName: String, val count: Int)

data class GrowthRow(val tag: String, val latest: Double?, val previous: Double?)

@Dao
interface SampleDao {
    @Insert
    suspend fun insertAll(samples: List<SampleEntity>)

    /** Average per time bucket, so long windows stay light to draw. */
    @Query(
        "SELECT (ts / :bucketMs) * :bucketMs AS bucket, AVG(value) AS value FROM sample " +
            "WHERE metric = :metric AND tag = :tag AND ts >= :since GROUP BY bucket ORDER BY bucket",
    )
    suspend fun series(metric: String, tag: String, since: Long, bucketMs: Long): List<SeriesPoint>

    @Query(
        "SELECT (ts / :bucketMs) * :bucketMs AS bucket, AVG(value) AS value FROM sample " +
            "WHERE metric = :metric AND tag = :tag AND ts >= :start AND ts < :end GROUP BY bucket ORDER BY bucket",
    )
    suspend fun seriesBetween(metric: String, tag: String, start: Long, end: Long, bucketMs: Long): List<SeriesPoint>

    @Query("SELECT AVG(value) FROM sample WHERE metric = :metric AND tag = :tag AND ts >= :start AND ts < :end")
    suspend fun average(metric: String, tag: String, start: Long, end: Long): Double?

    @Query("SELECT * FROM sample ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<SampleEntity>

    /** Latest value per tag and the value at [before] (or the oldest one when the tag is more recent). */
    @Query(
        "SELECT s.tag AS tag, " +
            "(SELECT l.value FROM sample l WHERE l.metric = :metric AND l.tag = s.tag ORDER BY l.ts DESC LIMIT 1) AS latest, " +
            "COALESCE(" +
            "(SELECT p.value FROM sample p WHERE p.metric = :metric AND p.tag = s.tag AND p.ts <= :before ORDER BY p.ts DESC LIMIT 1), " +
            "(SELECT f.value FROM sample f WHERE f.metric = :metric AND f.tag = s.tag ORDER BY f.ts ASC LIMIT 1)" +
            ") AS previous " +
            "FROM sample s WHERE s.metric = :metric GROUP BY s.tag",
    )
    suspend fun growth(metric: String, before: Long): List<GrowthRow>

    @Query("SELECT DISTINCT tag FROM sample WHERE metric = :metric AND ts >= :since ORDER BY tag")
    suspend fun tags(metric: String, since: Long): List<String>

    @Query("SELECT MIN(ts) FROM sample")
    suspend fun oldestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM sample")
    suspend fun count(): Long

    @Query("DELETE FROM sample WHERE ts < :before")
    suspend fun deleteBefore(before: Long): Int
}

@Dao
interface ChargeSessionDao {
    @Insert
    suspend fun insert(session: ChargeSessionEntity): Long

    @Update
    suspend fun update(session: ChargeSessionEntity)

    @Query("SELECT * FROM charge_session ORDER BY startTs DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<ChargeSessionEntity>>

    @Query("SELECT * FROM charge_session ORDER BY startTs")
    suspend fun all(): List<ChargeSessionEntity>

    @Query("SELECT estimatedCapacityMah FROM charge_session WHERE estimatedCapacityMah IS NOT NULL ORDER BY startTs DESC LIMIT :limit")
    suspend fun recentCapacityEstimates(limit: Int): List<Double>

    @Query("DELETE FROM charge_session WHERE id = :id")
    suspend fun delete(id: Long)

    /** Closes sessions left open when the service was killed. */
    @Query("UPDATE charge_session SET endTs = lastTs WHERE endTs IS NULL")
    suspend fun closeDangling()

    @Query("DELETE FROM charge_session WHERE startTs < :before")
    suspend fun deleteBefore(before: Long): Int
}

@Dao
interface ThermalEpisodeDao {
    @Insert
    suspend fun insert(episode: ThermalEpisodeEntity): Long

    @Update
    suspend fun update(episode: ThermalEpisodeEntity)

    @Query("SELECT * FROM thermal_episode ORDER BY startTs DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<ThermalEpisodeEntity>>

    @Query("SELECT * FROM thermal_episode ORDER BY startTs")
    suspend fun all(): List<ThermalEpisodeEntity>

    @Query("SELECT COUNT(*) FROM thermal_episode WHERE startTs >= :start AND startTs < :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Query("UPDATE thermal_episode SET endTs = lastTs WHERE endTs IS NULL")
    suspend fun closeDangling()

    @Query("DELETE FROM thermal_episode WHERE startTs < :before")
    suspend fun deleteBefore(before: Long): Int
}

@Dao
interface JournalDao {
    @Insert
    suspend fun insertAll(events: List<JournalEventEntity>)

    @Query("SELECT * FROM journal_event ORDER BY ts DESC, id DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<JournalEventEntity>>

    @Query("SELECT * FROM journal_event ORDER BY ts")
    suspend fun all(): List<JournalEventEntity>

    @Query("SELECT * FROM journal_event WHERE type = :type AND subtype = :subtype ORDER BY ts DESC LIMIT 1")
    suspend fun latest(type: String, subtype: String): JournalEventEntity?

    @Query("SELECT COUNT(*) FROM journal_event WHERE type = :type AND ts >= :start AND ts < :end")
    suspend fun countBetween(type: String, start: Long, end: Long): Int

    @Query(
        "SELECT packageName, COUNT(*) AS count FROM journal_event WHERE type = :type AND ts >= :since " +
            "AND packageName IS NOT NULL GROUP BY packageName ORDER BY count DESC LIMIT :limit",
    )
    suspend fun topPackages(type: String, since: Long, limit: Int): List<PackageCount>

    @Query("DELETE FROM journal_event WHERE ts < :before")
    suspend fun deleteBefore(before: Long): Int
}

@Dao
interface PackageSnapshotDao {
    @Query("SELECT * FROM package_snapshot")
    suspend fun all(): List<PackageSnapshotEntity>

    @Query("DELETE FROM package_snapshot")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<PackageSnapshotEntity>)
}

@Database(
    entities = [
        SampleEntity::class,
        ChargeSessionEntity::class,
        ThermalEpisodeEntity::class,
        JournalEventEntity::class,
        PackageSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun samples(): SampleDao
    abstract fun chargeSessions(): ChargeSessionDao
    abstract fun thermalEpisodes(): ThermalEpisodeDao
    abstract fun journal(): JournalDao
    abstract fun packageSnapshots(): PackageSnapshotDao

    companion object {
        fun create(context: Context): HistoryDatabase =
            Room.databaseBuilder(context, HistoryDatabase::class.java, "history.db")
                // Schema changes during development reset the history rather than crash.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
