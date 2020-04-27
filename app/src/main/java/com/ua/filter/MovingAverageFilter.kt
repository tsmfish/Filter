package com.ua.filter

import com.github.mikephil.charting.data.Entry

class MovingAverageFilter(data: List<Entry>, windowSize: Int) {
    val averages by lazy {
        val result = ArrayList<Entry>()
        val sum = ArrayList<Float>(data.size).apply { add(0.0F) }
        for(i in 1 until data.size) {
            sum.add(sum[i-1] + data[i].y)
            if (i >= windowSize) {
                result.add(Entry(data[i].x,(sum[i] - sum[i - windowSize]) / windowSize))
            }
        }

        return@lazy result
    }
}