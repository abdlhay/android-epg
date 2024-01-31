package com.abmo.tvepg

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.abmo.tvepg.epg.domain.EPGChannel
import com.abmo.tvepg.epg.domain.EPGEvent
import com.abmo.tvepg.epg.EPG
import com.abmo.tvepg.epg.EPGClickListener
import com.abmo.tvepg.epg.misc.EPGDataImpl
import com.abmo.tvepg.epg.misc.MockDataService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var epg: EPG
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        epg = findViewById(R.id.epg)
        epg.setEPGClickListener(object : EPGClickListener {
            override fun onChannelClicked(channelPosition: Int, epgChannel: EPGChannel?) {
                Toast.makeText(
                    this@MainActivity,
                    epgChannel?.name + " clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onEventClicked(
                channelPosition: Int,
                programPosition: Int,
                epgEvent: EPGEvent?
            ) {
                Toast.makeText(
                    this@MainActivity,
                    epgEvent?.title + " clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onResetButtonClicked() {
                epg.recalculateAndRedraw(true)
            }


        })


        // Do initial load of data.
        loadEPGData()
    }

    override fun onDestroy() {
        epg.clearEPGImageCache()
        job.cancel()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun loadEPGData() {
        coroutineScope.launch {
            val epgData = withContext(Dispatchers.IO) {
                EPGDataImpl(MockDataService.mockData)
            }
            epg.setEPGData(epgData)
            epg.recalculateAndRedraw(false)
        }
    }


}