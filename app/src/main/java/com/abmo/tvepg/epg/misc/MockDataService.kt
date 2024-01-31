package com.abmo.tvepg.epg.misc

import com.abmo.tvepg.epg.EPG
import com.abmo.tvepg.epg.domain.EPGChannel
import com.abmo.tvepg.epg.domain.EPGEvent
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.util.Random


/**
 * Created by Kristoffer on 15-05-24.
 */
object MockDataService {
    private val rand = Random()
    private val availableEventLength: List<Int> = Lists.newArrayList(
        1000 * 60 * 15,  // 15 minutes
        1000 * 60 * 30,  // 30 minutes
        1000 * 60 * 45,  // 45 minutes
        1000 * 60 * 60,  // 60 minutes
        1000 * 60 * 120 // 120 minutes
    )
    private val availableEventTitles: List<String> = Lists.newArrayList(
        "Avengers",
        "How I Met Your Mother",
        "Silicon Valley",
        "Late Night with Jimmy Fallon",
        "The Big Bang Theory",
        "Leon",
        "Die Hard"
    )
    private val availableChannelLogos: List<String> = Lists.newArrayList(
        "https://camo.githubusercontent.com/9f03ff3f75821ce42fbf5881f4e26d1e266689df730a47e6f693904ebe3d4358/68747470733a2f2f692e696d6775722e636f6d2f4a644b787363732e706e67",
        "https://camo.githubusercontent.com/22faca2450b5d423eb8252aa9b0e43970db279b7729ef259c5611c5481e5b274/68747470733a2f2f692e696d6775722e636f6d2f507234697869412e706e67",
        "https://camo.githubusercontent.com/bb23cd10a497f0c183849ff7f1a83a8feaabbfceb54c3822d677c3c6be7357a9/68747470733a2f2f692e696d6775722e636f6d2f536b66367664692e706e67",
        "https://camo.githubusercontent.com/604a06706d579034044aff58e5d59864e2a20dd896e40d2cde2e21c300293082/68747470733a2f2f75706c6f61642e77696b696d656469612e6f72672f77696b6970656469612f636f6d6d6f6e732f7468756d622f322f32642f4e6577735f32345f253238416c62616e69612532392e7376672f3130323470782d4e6577735f32345f253238416c62616e69612532392e7376672e706e67",
        "https://camo.githubusercontent.com/8fea0016633d787baa49a3631938b581ba5a579eb26a520748164966ffdcf340/68747470733a2f2f692e696d6775722e636f6d2f347a56796a314d2e706e67"
    )
    val mockData: HashMap<EPGChannel, List<EPGEvent>>
        get() {
            val result: HashMap<EPGChannel, List<EPGEvent>> =
                Maps.newLinkedHashMap()
            val nowMillis = System.currentTimeMillis()
            for (i in 0..19) {
                val epgChannel = EPGChannel(
                    availableChannelLogos[i % 5],
                    "Channel " + (i + 1), i.toString()
                )
                result[epgChannel] = createEvents(nowMillis)
            }
            return result
        }

    private fun createEvents(nowMillis: Long): List<EPGEvent> {
        val result: MutableList<EPGEvent> = Lists.newArrayList()
        val epgStart: Long = nowMillis - EPG.DAYS_BACK_MILLIS
        val epgEnd: Long = nowMillis + EPG.DAYS_FORWARD_MILLIS
        var currentTime = epgStart
        while (currentTime <= epgEnd) {
            val eventEnd = getEventEnd(currentTime)
            val epgEvent = EPGEvent(
                currentTime, eventEnd,
                availableEventTitles[randomBetween(1, 6)]
            )
            result.add(epgEvent)
            currentTime = eventEnd
        }
        return result
    }

    private fun getEventEnd(eventStartMillis: Long): Long {
        val length = availableEventLength[randomBetween(0, 4)].toLong()
        return eventStartMillis + length
    }

    private fun randomBetween(start: Int, end: Int): Int {
        return start + rand.nextInt(end - start + 1)
    }
}
