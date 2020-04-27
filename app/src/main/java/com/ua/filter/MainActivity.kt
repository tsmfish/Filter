package com.ua.filter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import com.ua.filter.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val LOG_TAG by lazy { MainActivity::class.simpleName }
    private val REQUEST_CODE by lazy { 113 }

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
        binding.limit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                save()
            }
        })
    }

    private fun subscribeViewModel(viewModel: MainViewModel) {
        viewModel.originalData.observe(this, Observer { originalData ->
            Log.d(LOG_TAG, "originalData: ${originalData?.size ?: -1}")
            originalData?.let { redraw() }
        })
        viewModel.extrema.observe(this, Observer { extrema -> extrema?.let { redraw() } })
        viewModel.limit.observe(this, Observer { viewModel.implementFilter() })
        viewModel.median.observe(this, Observer { redraw() })
        viewModel.filteredData.observe(this, Observer { redraw() })
        viewModel.windowsSize.observe(this, Observer { viewModel.filterChange() })
    }

    private fun redraw() {
        val lineData = LineData()
        if (viewModel.extrema.value != null) {
            lineData.addDataSet(
                LineDataSet(viewModel.extrema.value?.maximums, "max").apply {
                    color = resources.getColor(android.R.color.holo_red_dark)
                })
            lineData.addDataSet(
                LineDataSet(viewModel.extrema.value?.minimums, "min").apply {
                    color = resources.getColor(android.R.color.holo_blue_dark)
                })
        }
        if (viewModel.median.value != null) {
            lineData.addDataSet(
                LineDataSet(viewModel.originalData.value?.size?.let {
                    ArrayList<Entry>(it).apply {
                        for (item in viewModel.originalData.value!!) {
                            add(Entry(item.x, viewModel.median.value!!))
                        }
                    }
                }, "median").apply {
                    color = resources.getColor(android.R.color.holo_purple)
                    lineWidth = 1.0F
                    setDrawCircles(false)
                }
            )
        }
        if (viewModel.originalData.value != null) {
            lineData.addDataSet(
                LineDataSet(viewModel.originalData.value, "").apply {
                    color = resources.getColor(android.R.color.holo_green_dark)
                }
            )
        }
        if (viewModel.filteredData.value != null) {
            lineData.addDataSet(
                LineDataSet(viewModel.filteredData.value, "W size: ${viewModel.windowsSize.value}").apply {
                    color = resources.getColor(android.R.color.holo_orange_dark)
                    lineWidth = 1.0F
                    setDrawCircles(false)
                }
            )
        }

        binding.chart.data = lineData
        binding.chart.invalidate()
    }

    fun save() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
        } else {
            SimpleDateFormat("MM.dd_HH.mm.ss").format(Date()).let { fileName ->
                binding.chart.saveToGallery(fileName, 100)
                Snackbar.make(binding.root, "Saved: $fileName", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun load() {
//        startActivityForResult(Intent.createChooser(Intent().apply {
//            type = "text/plain"
//            action = Intent.ACTION_GET_CONTENT
//        }, "Select a file"), 111)
        viewModel.load(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            111 -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    viewModel.load(data.data!!)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (!grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                    save()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
