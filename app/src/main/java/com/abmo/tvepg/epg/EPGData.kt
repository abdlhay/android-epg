package com.abmo.tvepg.epg

import com.abmo.tvepg.epg.domain.EPGChannel
import com.abmo.tvepg.epg.domain.EPGEvent

/**
 * Interface to implement and pass to EPG containing data to be used.
 * Implementation can be a simple as simple as a Map/List or maybe an Adapter.
 * Created by Kristoffer on 15-05-23.
 */
interface EPGData {
    fun getChannel(position: Int): EPGChannel?
    fun getEvents(channelPosition: Int): List<EPGEvent?>?
    fun getEvent(channelPosition: Int, programPosition: Int): EPGEvent?
    val channelCount: Int

    fun hasData(): Boolean
}

