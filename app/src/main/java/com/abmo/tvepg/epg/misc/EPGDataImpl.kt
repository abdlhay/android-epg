package com.abmo.tvepg.epg.misc

import com.abmo.tvepg.epg.EPGData
import com.abmo.tvepg.epg.domain.EPGChannel
import com.abmo.tvepg.epg.domain.EPGEvent
import com.google.common.collect.Lists


class EPGDataImpl(data: HashMap<EPGChannel, List<EPGEvent>>) :
    EPGData {
    private var channels: List<EPGChannel?> = Lists.newArrayList()
    private var events: List<List<EPGEvent>> = Lists.newArrayList()

    init {
        channels = Lists.newArrayList(data.keys)
        events = Lists.newArrayList(data.values)
    }

    override fun getChannel(position: Int): EPGChannel? {
        return channels[position]
    }

    override fun getEvents(channelPosition: Int): List<EPGEvent> {
        return events[channelPosition]
    }

    override fun getEvent(channelPosition: Int, programPosition: Int): EPGEvent {
        return events[channelPosition][programPosition]
    }

    override val channelCount: Int
        get() = channels.size

    override fun hasData(): Boolean {
        return channels.isNotEmpty()
    }
}
