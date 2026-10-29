package fr.tflag.droidboost.core.model

data class CpuTimes(val idle: Long, val total: Long) {
    companion object {
        const val TOTAL = -1
    }
}

data class CpuCore(
    val index: Int,
    val currentKhz: Long?,
    val minKhz: Long?,
    val maxKhz: Long?,
    val online: Boolean,
)

data class CpuCluster(val name: String, val cores: List<CpuCore>) {
    val maxKhz: Long? get() = cores.mapNotNull { it.maxKhz }.maxOrNull()
}

object CpuTopology {

    /**
     * Groups cores sharing the same maximum frequency (Tensor G3: 1 + 4 + 4).
     * Cores whose maximum is unknown end up in a single trailing group.
     */
    fun clusters(cores: List<CpuCore>): List<CpuCluster> {
        val known = cores.filter { it.maxKhz != null }.groupBy { it.maxKhz!! }.toSortedMap()
        val names = clusterNames(known.size)
        val groups = known.values.mapIndexed { i, members -> CpuCluster(names[i], members.sortedBy { it.index }) }
        val unknown = cores.filter { it.maxKhz == null }
        return if (unknown.isEmpty()) groups else groups + CpuCluster("Cœurs", unknown.sortedBy { it.index })
    }

    private fun clusterNames(count: Int): List<String> = when (count) {
        1 -> listOf("CPU")
        2 -> listOf("Économie", "Performance")
        3 -> listOf("Économie", "Équilibré", "Performance")
        else -> List(count) { "Cluster ${it + 1}" }
    }
}
