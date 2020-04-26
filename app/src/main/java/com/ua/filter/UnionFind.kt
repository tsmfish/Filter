package health.detech.android.utils


class UnionFind(val size: Int) {
    private val LOG_TAG by lazy { UnionFind::class.simpleName }

    companion object {
        const val NO_SET = -1
    }

    private val domain: ArrayList<Int> = ArrayList(size)
    private var numSets = 0

    init {
        for (i in 0 until size) {
            domain.add(NO_SET)
        }
    }

    fun makeSet(idx: Int) {
        if (domain[idx] != NO_SET) throw IllegalArgumentException("set with $idx already exists")
        domain[idx] = idx
        numSets += 1
    }

    private fun extendSet(idxFrom: Int, idxTo: Int) {
        if (domain[idxTo] != NO_SET) throw IllegalArgumentException("set with $idxTo already exists")
        if (domain[idxFrom] == NO_SET) throw IllegalArgumentException("can't extends set with $idxFrom without exist")

        find(idxFrom).let { idxRoot ->
            domain[idxTo] = idxRoot
            domain[idxFrom] = idxRoot
        }
    }

    fun extendSetById(idxRoot: Int, idxTo: Int) {
//        Log.d(LOG_TAG, "call extendSetById($idxRoot, $idxTo)")
//        Log.d(LOG_TAG, "$domain")
        if (domain[idxTo] != NO_SET) throw IllegalArgumentException("You cannot call ExtendSetByID($idxTo) on an existing set.")
        if (domain[idxRoot] == NO_SET) throw IllegalArgumentException("You cannot call ExtendSetByID($idxRoot) without an existing set.t")
        find(idxRoot).let {
            if (it != idxRoot) {
                throw IllegalArgumentException("Please call the function ExtendSetByID($idxRoot) with an existing set ID.")
            } else {
                domain[idxTo] = idxRoot
            }
        }
    }

    fun union(idxSource: Int, idxDestination: Int) {
        val setIDB = findAndCompress(idxDestination)
        val isSameSet = findMergeCompressTo(idxSource, setIDB)
        if (!isSameSet) {
            numSets -= 1
        }
        if (numSets < 0) {
            throw IllegalStateException("We should have at least one set.")
        }
    }

    /**
     *      Finds the ID of the set to which the element @idxElement belongs.
     *      This function does not use compression, and therefore does not change any underlying data.
     */
    fun find(idx: Int): Int {
//        Log.d(LOG_TAG, "call find($idx)")
        if (domain[idx] == NO_SET) { return NO_SET }
        // ~ Find the root of the set
        var idxElement = idx
        while (true) {
            idxElement = domain[idxElement]
            if (idxElement == domain[idxElement]) return idxElement
        }
    }

    private fun findAndCompress(idx: Int): Int {
        if (domain[idx] == NO_SET) return NO_SET
        val path = arrayListOf(idx)
        var idxElement = idx
        while (true) {
            idxElement = domain[idxElement]
            path.add(idxElement)
            if (idxElement == domain[idxElement]) break
        }

        for (idxStep in path) {
            domain[idxStep] = idxElement
        }

        return idxElement
    }

    private fun findMergeCompressTo(idx: Int, idxRoot: Int): Boolean {
        if (domain[idx] == NO_SET) return false

        val path = arrayListOf(idx)
        var idxElement = idx
        while (true) {
            idxElement = domain[idxElement]
            path.add(idxElement)
            if (idxElement == domain[idxElement]) break
        }

        val isSameSet = (idxElement == idxRoot)
        for (idxStep in path) {
            domain[idxStep] = idxRoot
        }

        return isSameSet
    }

    override fun toString(): String {
        return "UnionFind(size=$size, domain=$domain, numSets=$numSets)"
    }
}