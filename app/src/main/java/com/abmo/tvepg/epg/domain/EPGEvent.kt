package com.abmo.tvepg.epg.domain

data class EPGEvent(
    val start: Long,
    val end: Long,
    val title: String) {

    val isCurrent: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in start..end
        }
}
