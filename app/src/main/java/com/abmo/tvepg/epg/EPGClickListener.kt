package com.abmo.tvepg.epg

import com.abmo.tvepg.epg.domain.EPGChannel
import com.abmo.tvepg.epg.domain.EPGEvent


/**
 * Created by Kristoffer on 15-05-25.
 */
interface EPGClickListener {
    fun onChannelClicked(channelPosition: Int, epgChannel: EPGChannel?)
    fun onEventClicked(channelPosition: Int, programPosition: Int, epgEvent: EPGEvent?)
    fun onResetButtonClicked()
}
