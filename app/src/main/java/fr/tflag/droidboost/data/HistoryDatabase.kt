package fr.tflag.droidboost.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
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

    @Query("UPDATE thermal_episode SET endTs = lastTs WHERE endTs IS NULL")
    suspend fun closeDangling()

    @Query("DELETE FROM thermal_episode WHERE startTs < :before")
    suspend fun deleteBefore(before: Long): Int
}

@Database(
    entities = [SampleEntity::class, ChargeSessionEntity::class, ThermalEpisodeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun samples(): SampleDao
    abstract fun chargeSessions(): ChargeSessionDao
    abstract fun thermalEpisodes(): ThermalEpisodeDao

    companion object {
        fun create(context: Context): HistoryDatabase =
            Room.databaseBuilder(context, HistoryDatabase::class.java, "history.db")
                // Schema changes during development reset the history rather than crash.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
