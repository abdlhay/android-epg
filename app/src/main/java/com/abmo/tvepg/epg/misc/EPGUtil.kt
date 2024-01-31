package com.abmo.tvepg.epg.misc

import android.content.Context
import android.util.Log
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat


/**
 * Created by Kristoffer.
 */
object EPGUtil {
    private const val TAG = "EPGUtil"
    private val dtfShortTime = DateTimeFormat.forPattern("HH:mm")
    fun getShortTime(timeMillis: Long): String {
        return dtfShortTime.print(timeMillis)
    }

    fun getWeekdayName(dateMillis: Long): String {
        val date = LocalDate(dateMillis)
        return date.dayOfWeek().asText
    }

    fun loadImageInto(context: Context?, url: String?, width: Int, height: Int, target: Target?) {
        if (context != null && target != null) {
            val picasso = initPicasso(context)
            picasso.load(url)
                .resize(width, height)
                .centerInside()
                .into(target)
        }
    }




    private fun initPicasso(context: Context): Picasso {
        return Picasso.Builder(context)
                .listener { _, _, exception ->
                    Log.e(
                        TAG,
                        exception.message!!
                    )
                }
                .build()
    }
}
