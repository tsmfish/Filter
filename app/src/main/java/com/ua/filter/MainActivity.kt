package com.ua.filter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.ua.filter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val LOG_TAG by lazy { MainActivity::class.simpleName }

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
            viewModel = this@MainActivity.viewModel
            view = this@MainActivity
            binding = this
        }
        subscribeViewModel(viewModel)
    }

    private fun subscribeViewModel(viewModel: MainViewModel) {
        viewModel.originalData.observe(this, Observer { originalData ->
            Log.d(LOG_TAG, "originalData: ${originalData?.size ?: -1}")
            originalData?.let {
                binding.chart.data = LineData(LineDataSet(it, "data").apply {
                    isHighlightEnabled = true
                    color = resources.getColor(android.R.color.holo_green_dark)
                })
                binding.chart.invalidate()
            }
        })
        viewModel.extrema.observe(this, Observer { extrema ->
            extrema?.let {
                binding.chart.data = LineData(
                    LineDataSet(viewModel.originalData.value, "").apply {
                        color = resources.getColor(android.R.color.holo_green_dark)
                    },
                    LineDataSet(extrema.maximums, "max").apply {
                        color = resources.getColor(android.R.color.holo_red_dark)
                    },
                    LineDataSet(extrema.minimums, "min").apply {
                        color = resources.getColor(android.R.color.holo_blue_dark)
                    }
                )
                binding.chart.invalidate()
            }
        })
        viewModel.limit.observe(this, Observer { viewModel.implementFilter() })
    }

    fun load() {
//        startActivityForResult(Intent.createChooser(Intent().apply {
//            type = "text/plain"
//            action = Intent.ACTION_GET_CONTENT
//        }, "Select a file"), 111)
        viewModel.load(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            111 -> { if (resultCode == Activity.RESULT_OK && data?.data != null) {
                viewModel.load(data.data!!)
            }}
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
