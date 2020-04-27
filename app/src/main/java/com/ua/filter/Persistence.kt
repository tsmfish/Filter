package health.detech.android.utils

import android.util.Log
import com.github.mikephil.charting.data.Entry

class Persistence(val data: List<Entry>) {
    private val LOG_TAG by lazy { Persistence::class.simpleName }

    private val dataSize by lazy { data.size }
    private val idxSorted by lazy {
        ArrayList<Int>(data.size).apply {
            this.addAll(0 until dataSize)
            this.sortWith(compareBy { data[it].y })
        }
    }
    private val unionFind by lazy { UnionFind(dataSize) }
    val median by lazy {
        data.toSet().sortedBy { it.y }.let {
            when (it.size % 2) {
                0 -> it[it.size / 2].y
                1 -> (it[it.size / 2].y + it[it.size / 2 + 1].y) / 2
                else -> throw IllegalStateException()
            }
        }
    }
    private val _minimums by lazy { ArrayList<Extreme>() }
    private val _maximums by lazy { ArrayList<Extreme>() }
    val minimums
        get() = when (extremaAndPersistence.size) {
            0 -> null
            1 -> extremaAndPersistence
            else -> _minimums
        }
    val maximums
        get() = when (extremaAndPersistence.size) {
            0 -> null
            1 -> extremaAndPersistence
            else -> _maximums
        }


    val extremaAndPersistence by lazy {
        ArrayList<Extreme>().apply {
            Log.d(LOG_TAG, "data.size[$dataSize]")
            when (dataSize) {
                0 -> return@apply
                1 -> this.add(Extreme(0, data[0], 0.0F))
                else -> {
//                    Log.d(LOG_TAG, "idxSorted: ($idxSorted)")
//                    Log.d(LOG_TAG, "$unionFind.")
                    for (idx in idxSorted) {
//                        Log.d(LOG_TAG, "iterate with idx: $idx")
//                        Log.d(LOG_TAG, "$unionFind")
                        // ~ Get neighborhood indices
                        val leftIdx = kotlin.math.max(idx - 1, 0)
                        val rightIdx = kotlin.math.min(idx + 1, dataSize - 1)
//                        Log.d(LOG_TAG, "leftIdx: $leftIdx, rightIdx: $rightIdx")

                        // ~ Count number of components in neighborhood
                        val neighborComponents = ArrayList<Int>()
                        unionFind.find(leftIdx).let {
                            if (it != UnionFind.NO_SET) {
                                neighborComponents.add(it)
//                                Log.d(LOG_TAG, "LeftNeighborComponent: $it")
                            }
                        }
                        unionFind.find(rightIdx).let {
                            if (it != UnionFind.NO_SET) {
                                neighborComponents.add(it)
//                                Log.d(LOG_TAG, "RightNeighborComponent: $it")
                            }
                        }

                        // ~ Left and Right cannot be the same set in a 1D domain
                        // ~ self._assert(LeftNeighborComponent == UnionFind.NOSET or RightNeighborComponent == UnionFind.NOSET or LeftNeighborComponent != RightNeighborComponent, "Left and Right cannot be the same set in a 1D domain.")
                        when (neighborComponents.size) {
                            0 -> {
                                // ~ Create a new component
                                unionFind.makeSet(idx)
                            }
                            1 -> {
                                // ~ Extend the one and only component in the neighborhood
                                // ~ Note that NeighborComponents[0] holds the root of a component, since we called Find() earlier to retrieve it
                                unionFind.extendSetById(neighborComponents[0], idx)
                            }
                            else -> {
                                // ~ Merge the two components on either side of the current point
                                // ~ The current point is a maximum. We look for the largest minimum on either side to pair with. That is the smallest hub.
                                // ~ We look for the lowest minimum first (the one that survives) to break the tie in case of equality: np.argmin returns the first occurence in this case.
//                        val idxLowestNeighborComp = np.argmin(InputData[NeighborComponents])
                                val idxLowestNeighborComp = argMin(neighborComponents)
//                                Log.d(LOG_TAG, "idxLowestNeighborComp: $idxLowestNeighborComp")
                                val idxLowestMinimum = neighborComponents[idxLowestNeighborComp]
                                val idxHighestMinimum =
                                    neighborComponents[(idxLowestNeighborComp + 1) % 2]
//                                Log.d(LOG_TAG, "idxLowestMinimum: $idxLowestMinimum, idxHighestMinimum: $idxHighestMinimum")
                                unionFind.extendSetById(idxLowestMinimum, idx)
                                unionFind.union(idxHighestMinimum, idxLowestMinimum)

                                // ~ Record the two paired extrema: index of minimu, index of maximum, persistence value
                                (data[idx].y - data[idxHighestMinimum].y).let {
                                    this.add(
                                        Extreme(
                                            idxHighestMinimum,
                                            data[idxHighestMinimum],
                                            it
//                                            kotlin.math.abs(data[idxHighestMinimum].y - median)
                                        )
                                    )
                                    this.add(
                                        Extreme(
                                            idx,
                                            data[idx],
                                            it
//                                            kotlin.math.abs(data[idx].y - median)
                                        )
                                    )
                                    _minimums.add(this[this.size - 2])
                                    _maximums.add(this[this.size - 1])
                                }
                            }
                        }
                    }
//                    this.sortBy { it.value.x }
                }
            }
//            Log.d(LOG_TAG, "Persistence.size: ${this.size}")
//            Log.d(LOG_TAG, "$this")
        }
    }

    private fun argMin(source: List<Int>): Int {
//        Log.d(LOG_TAG, "Persistence::argMin($source)")
        val data = ArrayList<Entry>(source.size).apply {
            for (item in source) {
                this.add(data[item])
            }
        }
//        Log.d(LOG_TAG, "Persistence::argMin(): data: $data")
        var minIdx = 0
        var value = data[minIdx].y
        for (idx in 1 until data.size) {
            if (value > data[idx].y) {
                minIdx = idx
                value = data[minIdx].y
            }
        }
//        Log.d(LOG_TAG, "Persistence::argMin(): $minIdx")
        return minIdx
    }


    val min by lazy { extremaAndPersistence.minBy { it.value.y } }
    val max by lazy { extremaAndPersistence.maxBy { it.value.y } }
    fun getFiltered(persistence: Float) =
        extremaAndPersistence.filter { it.persistence > persistence }

    data class Extreme(val index: Int, val value: Entry, val persistence: Float)
}