package fr.tflag.droidboost.journal

import android.content.Context
import android.net.Uri
import fr.tflag.droidboost.data.HistoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Exports the whole local history as a ZIP of CSV files, to a document picked by the user. */
class Exporter(private val context: Context, private val db: HistoryDatabase) {

    suspend fun exportZip(target: Uri): Int = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(target) ?: error("Fichier de destination inaccessible")
        var rows = 0
        ZipOutputStream(stream.buffered()).use { zip ->
            rows += zip.csv("mesures.csv", "horodatage,metrique,etiquette,valeur") { out ->
                var offset = 0
                var count = 0
                while (true) {
                    val page = db.samples().page(PAGE, offset)
                    if (page.isEmpty()) break
                    page.forEach { out.line(iso(it.ts), it.metric, it.tag, it.value.toString()) }
                    count += page.size
                    offset += page.size
                }
                count
            }
            rows += zip.csv("journal.csv", "horodatage,type,sous_type,paquet,titre,details") { out ->
                val events = db.journal().all()
                events.forEach { out.line(iso(it.ts), it.type, it.subtype.orEmpty(), it.packageName.orEmpty(), it.title, it.details.orEmpty()) }
                events.size
            }
            rows += zip.csv(
                "sessions_de_charge.csv",
                "debut,fin,niveau_debut,niveau_fin,mah_ajoutes,capacite_estimee_mah,puissance_moy_w,puissance_max_w,temperature_max_c",
            ) { out ->
                val sessions = db.chargeSessions().all()
                sessions.forEach {
                    out.line(
                        iso(it.startTs), it.endTs?.let(::iso).orEmpty(), it.startLevel.toString(), it.endLevel.toString(),
                        it.addedMah?.toString().orEmpty(), it.estimatedCapacityMah?.toString().orEmpty(),
                        it.averagePowerW?.toString().orEmpty(), it.maxPowerW.toString(), it.maxTemperatureC.toString(),
                    )
                }
                sessions.size
            }
            rows += zip.csv("episodes_thermiques.csv", "debut,fin,statut_max,marge_max,appli,en_charge") { out ->
                val episodes = db.thermalEpisodes().all()
                episodes.forEach {
                    out.line(
                        iso(it.startTs), it.endTs?.let(::iso).orEmpty(), it.maxStatus.toString(),
                        it.maxHeadroom?.toString().orEmpty(), it.foregroundPackage.orEmpty(), it.charging.toString(),
                    )
                }
                episodes.size
            }
        }
        rows
    }

    private inline fun ZipOutputStream.csv(name: String, header: String, body: (OutputStream) -> Int): Int {
        putNextEntry(ZipEntry(name))
        write(header.toByteArray())
        write("\n".toByteArray())
        val count = body(this)
        closeEntry()
        return count
    }

    private fun OutputStream.line(vararg cells: String) {
        write(cells.joinToString(",") { escape(it) }.toByteArray())
        write("\n".toByteArray())
    }

    private fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell

    private fun iso(epochMs: Long): String = Instant.ofEpochMilli(epochMs).toString()

    private companion object {
        const val PAGE = 5_000
    }
}
