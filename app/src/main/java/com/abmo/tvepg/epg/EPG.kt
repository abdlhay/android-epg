package com.abmo.tvepg.epg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Scroller
import androidx.core.content.ContextCompat
import com.abmo.tvepg.R
import com.abmo.tvepg.epg.domain.EPGEvent
import com.abmo.tvepg.epg.misc.EPGUtil
import com.google.common.collect.Maps
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import org.joda.time.LocalDateTime
import java.lang.Exception
import kotlin.math.abs


/**
 * Classic EPG, electronic program guide, that scrolls both horizontal, vertical and diagonal.
 * It utilize onDraw() to draw the graphic on screen. So there are some private helper methods calculating positions etc.
 * Listed on Y-axis are channels and X-axis are programs/events. Data is added to EPG by using setEPGData()
 * and pass in an EPGData implementation. A click listener can be added using setEPGClickListener().
 * Created by Kristoffer, http://kmdev.se
 */
class EPG @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val mClipRect: Rect
    private val mDrawingRect: Rect
    private val mMeasuringRect: Rect
    private val mPaint: Paint
    private val mScroller: Scroller
    private val mGestureDetector: GestureDetector
    private val mChannelLayoutMargin: Int
    private val mChannelLayoutPadding: Int
    private val mChannelLayoutHeight: Int
    private val mChannelLayoutWidth: Int
    private val mChannelLayoutBackground: Int
    private val mEventLayoutBackground: Int
    private val mEventLayoutBackgroundCurrent: Int
    private val mEventLayoutTextColor: Int
    private val mEventLayoutTextSize: Int
    private val mTimeBarLineWidth: Int
    private val mTimeBarLineColor: Int
    private val mTimeBarHeight: Int
    private val mTimeBarTextSize: Int
    private val mResetButtonSize: Int
    private val mResetButtonMargin: Int
    private val mResetButtonIcon: Bitmap
    private val mEPGBackground: Int
    private val mChannelImageCache: MutableMap<String, Bitmap>
    private val mChannelImageTargetCache: MutableMap<String, Target>
    private var mClickListener: EPGClickListener? = null
    private var mMaxHorizontalScroll = 0
    private var mMaxVerticalScroll = 0
    private var mMillisPerPixel: Long = 0
    private var mTimeOffset: Long = 0
    private var mTimeLowerBoundary: Long = 0
    private var mTimeUpperBoundary: Long = 0
    private var epgData: EPGData? = null

    init {
        setWillNotDraw(false)
        resetBoundaries()
        mDrawingRect = Rect()
        mClipRect = Rect()
        mMeasuringRect = Rect()
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mGestureDetector = GestureDetector(context, OnGestureListener())
        mChannelImageCache = Maps.newHashMap()
        mChannelImageTargetCache = Maps.newHashMap()

        // Adding some friction that makes the epg less flappy.
        mScroller = Scroller(context)
        mScroller.setFriction(0.2f)
        mEPGBackground = ContextCompat.getColor(context, R.color.epg_background)
        mChannelLayoutMargin = resources.getDimensionPixelSize(R.dimen.epg_channel_layout_margin)
        mChannelLayoutPadding = resources.getDimensionPixelSize(R.dimen.epg_channel_layout_padding)
        mChannelLayoutHeight = resources.getDimensionPixelSize(R.dimen.epg_channel_layout_height)
        mChannelLayoutWidth = resources.getDimensionPixelSize(R.dimen.epg_channel_layout_width)
        mChannelLayoutBackground = ContextCompat.getColor(context,
            R.color.epg_channel_layout_background
        )
        mEventLayoutBackground = ContextCompat.getColor(context,
            R.color.epg_event_layout_background
        )
        mEventLayoutBackgroundCurrent =
            ContextCompat.getColor(context, R.color.epg_event_layout_background_current)
        mEventLayoutTextColor = ContextCompat.getColor(context, R.color.epg_event_layout_text)
        mEventLayoutTextSize = resources.getDimensionPixelSize(R.dimen.epg_event_layout_text)
        mTimeBarHeight = resources.getDimensionPixelSize(R.dimen.epg_time_bar_height)
        mTimeBarTextSize = resources.getDimensionPixelSize(R.dimen.epg_time_bar_text)
        mTimeBarLineWidth = resources.getDimensionPixelSize(R.dimen.epg_time_bar_line_width)
        mTimeBarLineColor = ContextCompat.getColor(context, R.color.epg_time_bar)
        mResetButtonSize = resources.getDimensionPixelSize(R.dimen.epg_reset_button_size)
        mResetButtonMargin = resources.getDimensionPixelSize(R.dimen.epg_reset_button_margin)
        val options = BitmapFactory.Options()
        options.outWidth = mResetButtonSize
        options.outHeight = mResetButtonSize
        mResetButtonIcon = BitmapFactory.decodeResource(resources, R.drawable.reset, options)
    }

    override fun onDraw(canvas: Canvas) {
        if (epgData != null && epgData!!.hasData()) {
            mTimeLowerBoundary = getTimeFrom(scrollX)
            mTimeUpperBoundary = getTimeFrom(scrollX + width)
            val drawingRect = mDrawingRect
            drawingRect.left = scrollX
            drawingRect.top = scrollY
            drawingRect.right = drawingRect.left + width
            drawingRect.bottom = drawingRect.top + height
            drawChannelListItems(canvas, drawingRect)
            drawEvents(canvas, drawingRect)
            drawTimeBar(canvas, drawingRect)
            drawTimeLine(canvas, drawingRect)
            drawResetButton(canvas)

            // If scroller is scrolling/animating do scroll. This applies when doing a fling.
            if (mScroller.computeScrollOffset()) {
                scrollTo(mScroller.currX, mScroller.currY)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateAndRedraw(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}


    private fun drawResetButton(canvas: Canvas) {
        // Show button when scrolled 1/3 of screen width from current time
        val drawingRect: Rect
        val threshold = (width / 3).toLong()
        if (abs(xPositionStart - scrollX) > threshold) {
            drawingRect = calculateResetButtonHitArea()
            mPaint.color = mTimeBarLineColor
            canvas.drawCircle(
                (drawingRect.right - mResetButtonSize / 2).toFloat(),
                (
                        drawingRect.bottom - mResetButtonSize / 2).toFloat(),
                (
                        drawingRect.width().coerceAtMost(drawingRect.height()) / 2).toFloat(),
                mPaint
            )
            drawingRect.left += mResetButtonMargin
            drawingRect.right -= mResetButtonMargin
            drawingRect.top += mResetButtonMargin
            drawingRect.bottom -= mResetButtonMargin
            canvas.drawBitmap(mResetButtonIcon, null, drawingRect, mPaint)
        }
    }

    private fun drawTimeBarBottomStroke(canvas: Canvas, drawingRect: Rect) {
        drawingRect.left = scrollX
        drawingRect.top = scrollY + mTimeBarHeight
        drawingRect.right = drawingRect.left + width
        drawingRect.bottom = drawingRect.top + mChannelLayoutMargin

        // Bottom stroke
        mPaint.color = mEPGBackground
        canvas.drawRect(drawingRect, mPaint)
    }

    private fun drawTimeBar(canvas: Canvas, drawingRect: Rect) {
        drawingRect.left = scrollX + mChannelLayoutWidth + mChannelLayoutMargin
        drawingRect.top = scrollY
        drawingRect.right = drawingRect.left + width
        drawingRect.bottom = drawingRect.top + mTimeBarHeight
        mClipRect.left = scrollX + mChannelLayoutWidth + mChannelLayoutMargin
        mClipRect.top = scrollY
        mClipRect.right = scrollX + width
        mClipRect.bottom = mClipRect.top + mTimeBarHeight
        canvas.save()
        canvas.clipRect(mClipRect)

        // Background
        mPaint.color = mChannelLayoutBackground
        canvas.drawRect(drawingRect, mPaint)

        // Time stamps
        mPaint.color = mEventLayoutTextColor
        mPaint.textSize = mTimeBarTextSize.toFloat()
        for (i in 0 until HOURS_IN_VIEWPORT_MILLIS / TIME_LABEL_SPACING_MILLIS) {
            // Get time and round to nearest half hour
            val time = TIME_LABEL_SPACING_MILLIS *
                    ((mTimeLowerBoundary + TIME_LABEL_SPACING_MILLIS * i + TIME_LABEL_SPACING_MILLIS / 2) / TIME_LABEL_SPACING_MILLIS)
            canvas.drawText(
                EPGUtil.getShortTime(time),
                getXFrom(time).toFloat(),
                (
                        drawingRect.top + ((drawingRect.bottom - drawingRect.top) / 2 + mTimeBarTextSize / 2)).toFloat(),
                mPaint
            )
        }
        canvas.restore()
        drawTimeBarDayIndicator(canvas, drawingRect)
        drawTimeBarBottomStroke(canvas, drawingRect)
    }

    private fun drawTimeBarDayIndicator(canvas: Canvas, drawingRect: Rect) {
        drawingRect.left = scrollX
        drawingRect.top = scrollY
        drawingRect.right = drawingRect.left + mChannelLayoutWidth
        drawingRect.bottom = drawingRect.top + mTimeBarHeight

        // Background
        mPaint.color = mChannelLayoutBackground
        canvas.drawRect(drawingRect, mPaint)

        // Text
        mPaint.color = mEventLayoutTextColor
        mPaint.textSize = mTimeBarTextSize.toFloat()
        mPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            EPGUtil.getWeekdayName(mTimeLowerBoundary),
            (
                    drawingRect.left + (drawingRect.right - drawingRect.left) / 2).toFloat(),
            (
                    drawingRect.top + ((drawingRect.bottom - drawingRect.top) / 2 + mTimeBarTextSize / 2)).toFloat(),
            mPaint
        )
        mPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawTimeLine(canvas: Canvas, drawingRect: Rect) {
        val now = System.currentTimeMillis()
        if (shouldDrawTimeLine(now)) {
            drawingRect.left = getXFrom(now)
            drawingRect.top = scrollY
            drawingRect.right = drawingRect.left + mTimeBarLineWidth
            drawingRect.bottom = drawingRect.top + height
            mPaint.color = mTimeBarLineColor
            canvas.drawRect(drawingRect, mPaint)
        }
    }

    private fun drawEvents(canvas: Canvas, drawingRect: Rect) {
        val firstPos = firstVisibleChannelPosition
        val lastPos = lastVisibleChannelPosition
        for (pos in firstPos..lastPos) {

            // Set clip rectangle
            mClipRect.left = scrollX + mChannelLayoutWidth + mChannelLayoutMargin
            mClipRect.top = getTopFrom(pos)
            mClipRect.right = scrollX + width
            mClipRect.bottom = mClipRect.top + mChannelLayoutHeight
            canvas.save()
            canvas.clipRect(mClipRect)

            // Draw each event
            var foundFirst = false
            val epgEvents: List<EPGEvent?>? = epgData!!.getEvents(pos)
            if (epgEvents != null) {
                for (event in epgEvents) {
                    if (isEventVisible(event!!.start, event.end)) {
                        drawEvent(canvas, pos, event, drawingRect)
                        foundFirst = true
                    } else if (foundFirst) {
                        break
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun drawEvent(
        canvas: Canvas,
        channelPosition: Int,
        event: EPGEvent,
        drawingRect: Rect
    ) {
        setEventDrawingRectangle(channelPosition, event.start, event.end, drawingRect)

        // Background
        mPaint.color =
            if (event.isCurrent) mEventLayoutBackgroundCurrent else mEventLayoutBackground
        canvas.drawRect(drawingRect, mPaint)

        // Add left and right inner padding
        drawingRect.left += mChannelLayoutPadding
        drawingRect.right -= mChannelLayoutPadding

        // Text
        mPaint.color = mEventLayoutTextColor
        mPaint.textSize = mEventLayoutTextSize.toFloat()

        // Move drawing.top so text will be centered (text is drawn bottom>up)
        mPaint.getTextBounds(event.title, 0, event.title.length, mMeasuringRect)
        drawingRect.top += (drawingRect.bottom - drawingRect.top) / 2 + mMeasuringRect.height() / 2
        var title: String = event.title
        title = title.substring(
            0,
            mPaint.breakText(title, true, (drawingRect.right - drawingRect.left).toFloat(), null)
        )
        canvas.drawText(title, drawingRect.left.toFloat(), drawingRect.top.toFloat(), mPaint)
    }

    private fun setEventDrawingRectangle(
        channelPosition: Int,
        start: Long,
        end: Long,
        drawingRect: Rect
    ) {
        drawingRect.left = getXFrom(start)
        drawingRect.top = getTopFrom(channelPosition)
        drawingRect.right = getXFrom(end) - mChannelLayoutMargin
        drawingRect.bottom = drawingRect.top + mChannelLayoutHeight
    }

    private fun drawChannelListItems(canvas: Canvas, drawingRect: Rect) {
        // Background
        mMeasuringRect.left = scrollX
        mMeasuringRect.top = scrollY
        mMeasuringRect.right = drawingRect.left + mChannelLayoutWidth
        mMeasuringRect.bottom = mMeasuringRect.top + height
        mPaint.color = mChannelLayoutBackground
        canvas.drawRect(mMeasuringRect, mPaint)
        val firstPos = firstVisibleChannelPosition
        val lastPos = lastVisibleChannelPosition
        for (pos in firstPos..lastPos) {
            drawChannelItem(canvas, pos, drawingRect)
        }
    }

    private fun drawChannelItem(canvas: Canvas, position: Int, drawingRect: Rect) {
        drawingRect.left = scrollX
        drawingRect.top = getTopFrom(position)
        drawingRect.right = drawingRect.left + mChannelLayoutWidth
        drawingRect.bottom = drawingRect.top + mChannelLayoutHeight

        // Loading channel image into target for
        val imageURL: String = epgData?.getChannel(position)!!.imageURL
        if (mChannelImageCache.containsKey(imageURL)) {
            val image = mChannelImageCache[imageURL]
            canvas.drawBitmap(image!!, null, getDrawingRectForChannelImage(drawingRect, image), null)
        } else {
            val smallestSide = mChannelLayoutHeight.coerceAtMost(mChannelLayoutWidth)
            if (!mChannelImageTargetCache.containsKey(imageURL)) {
                mChannelImageTargetCache[imageURL] = object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                        mChannelImageCache[imageURL] = bitmap
                        redraw()
                        mChannelImageTargetCache.remove(imageURL)
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {

                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                    }
                }
                EPGUtil.loadImageInto(
                    context, imageURL, smallestSide, smallestSide,
                    mChannelImageTargetCache[imageURL]
                )
            }
        }
    }

    private fun getDrawingRectForChannelImage(drawingRect: Rect, image: Bitmap?): Rect {
        drawingRect.left += mChannelLayoutPadding
        drawingRect.top += mChannelLayoutPadding
        drawingRect.right -= mChannelLayoutPadding
        drawingRect.bottom -= mChannelLayoutPadding
        val imageWidth = image!!.width
        val imageHeight = image.height
        val imageRatio = imageHeight / imageWidth.toFloat()
        val rectWidth = drawingRect.right - drawingRect.left
        val rectHeight = drawingRect.bottom - drawingRect.top

        // Keep aspect ratio.
        if (imageWidth > imageHeight) {
            val padding = (rectHeight - rectWidth * imageRatio).toInt() / 2
            drawingRect.top += padding
            drawingRect.bottom -= padding
        } else if (imageWidth < imageHeight) {
            val padding = (rectWidth - rectHeight / imageRatio).toInt() / 2
            drawingRect.left += padding
            drawingRect.right -= padding
        }
        return drawingRect
    }

    private fun shouldDrawTimeLine(now: Long): Boolean {
        return now in mTimeLowerBoundary..<mTimeUpperBoundary
    }

    private fun isEventVisible(start: Long, end: Long): Boolean {
        return start in mTimeLowerBoundary..mTimeUpperBoundary || end in mTimeLowerBoundary..mTimeUpperBoundary || start <= mTimeLowerBoundary && end >= mTimeUpperBoundary
    }

    private fun calculatedBaseLine(): Long {
        return LocalDateTime.now().toDateTime().minusMillis(DAYS_BACK_MILLIS).millis
    }

    private val firstVisibleChannelPosition: Int
        get() {
            val y = scrollY
            var position = ((y - mChannelLayoutMargin - mTimeBarHeight)
                    / (mChannelLayoutHeight + mChannelLayoutMargin))
            if (position < 0) {
                position = 0
            }
            return position
        }

    private val lastVisibleChannelPosition: Int
        get() {
            val y = scrollY
            val totalChannelCount: Int = epgData!!.channelCount
            val screenHeight = height
            var position = ((y + screenHeight + mTimeBarHeight - mChannelLayoutMargin)
                    / (mChannelLayoutHeight + mChannelLayoutMargin))
            if (position > totalChannelCount - 1) {
                position = totalChannelCount - 1
            }

            // Add one extra row if we don't fill screen with current..
            return if (y + screenHeight > position * mChannelLayoutHeight && position < totalChannelCount - 1) position + 1 else position
        }

    private fun calculateMaxHorizontalScroll() {
        mMaxHorizontalScroll =
            ((DAYS_BACK_MILLIS + DAYS_FORWARD_MILLIS - HOURS_IN_VIEWPORT_MILLIS) / mMillisPerPixel).toInt()
    }

    private fun calculateMaxVerticalScroll() {
        val maxVerticalScroll = getTopFrom(epgData!!.channelCount - 2) + mChannelLayoutHeight
        mMaxVerticalScroll = if (maxVerticalScroll < height) 0 else maxVerticalScroll - height
    }

    private fun getXFrom(time: Long): Int {
        return (((time - mTimeOffset) / mMillisPerPixel).toInt() + mChannelLayoutMargin
                + mChannelLayoutWidth + mChannelLayoutMargin)
    }

    private fun getTopFrom(position: Int): Int {
        return position * (mChannelLayoutHeight + mChannelLayoutMargin) + mChannelLayoutMargin + mTimeBarHeight
    }

    private fun getTimeFrom(x: Int): Long {
        return x * mMillisPerPixel + mTimeOffset
    }

    private fun calculateMillisPerPixel(): Long {
        return (HOURS_IN_VIEWPORT_MILLIS / (resources.displayMetrics.widthPixels - mChannelLayoutWidth - mChannelLayoutMargin)).toLong()
    }

    private val xPositionStart: Int
        get() = getXFrom(System.currentTimeMillis() - HOURS_IN_VIEWPORT_MILLIS / 2)

    private fun resetBoundaries() {
        mMillisPerPixel = calculateMillisPerPixel()
        mTimeOffset = calculatedBaseLine()
        mTimeLowerBoundary = getTimeFrom(0)
        mTimeUpperBoundary = getTimeFrom(width)
    }

    private fun calculateChannelsHitArea(): Rect {
        mMeasuringRect.top = mTimeBarHeight
        val visibleChannelsHeight: Int =
            epgData!!.channelCount * (mChannelLayoutHeight + mChannelLayoutMargin)
        mMeasuringRect.bottom =
            if (visibleChannelsHeight < height) visibleChannelsHeight else height
        mMeasuringRect.left = 0
        mMeasuringRect.right = mChannelLayoutWidth
        return mMeasuringRect
    }

    private fun calculateProgramsHitArea(): Rect {
        mMeasuringRect.top = mTimeBarHeight
        val visibleChannelsHeight: Int =
            epgData!!.channelCount * (mChannelLayoutHeight + mChannelLayoutMargin)
        mMeasuringRect.bottom =
            if (visibleChannelsHeight < height) visibleChannelsHeight else height
        mMeasuringRect.left = mChannelLayoutWidth
        mMeasuringRect.right = width
        return mMeasuringRect
    }

    private fun calculateResetButtonHitArea(): Rect {
        mMeasuringRect.left = scrollX + width - mResetButtonSize - mResetButtonMargin
        mMeasuringRect.top = scrollY + height - mResetButtonSize - mResetButtonMargin
        mMeasuringRect.right = mMeasuringRect.left + mResetButtonSize
        mMeasuringRect.bottom = mMeasuringRect.top + mResetButtonSize
        return mMeasuringRect
    }

    private fun getChannelPosition(y: Int): Int {

        val channelPosition = (((y - mTimeBarHeight) + mChannelLayoutMargin)
                / (mChannelLayoutHeight + mChannelLayoutMargin))
        return if (epgData?.channelCount == 0) -1 else channelPosition
    }

    private fun getProgramPosition(channelPosition: Int, time: Long): Int {
        val events: List<EPGEvent?>? = epgData?.getEvents(channelPosition)
        if (events != null) {
            for (eventPos in events.indices) {
                val event: EPGEvent? = events[eventPos]
                if (event?.start!! <= time && event.end >= time) {
                    return eventPos
                }
            }
        }
        return -1
    }

    /**
     * Add click listener to the EPG.
     * @param epgClickListener to add.
     */
    fun setEPGClickListener(epgClickListener: EPGClickListener?) {
        mClickListener = epgClickListener
    }

    /**
     * Add data to EPG. This must be set for EPG to able to draw something.
     * @param epgData pass in any implementation of EPGData.
     */
    fun setEPGData(epgData: EPGData?) {
        this.epgData = epgData
    }

    /**
     * This will recalculate boundaries, maximal scroll and scroll to start position which is current time.
     * To be used on device rotation etc since the device height and width will change.
     * @param withAnimation true if scroll to current position should be animated.
     */
    fun recalculateAndRedraw(withAnimation: Boolean) {
        if (epgData != null && epgData!!.hasData()) {
            resetBoundaries()
            calculateMaxVerticalScroll()
            calculateMaxHorizontalScroll()
            mScroller.startScroll(
                scrollX, scrollY,
                xPositionStart - scrollX,
                0, if (withAnimation) 600 else 0
            )
            redraw()
        }
    }

    /**
     * Does a invalidate() and requestLayout() which causes a redraw of screen.
     */
    fun redraw() {
        invalidate()
        requestLayout()
    }

    /**
     * Clears the local image cache for channel images. Can be used when leaving epg and you want to
     * free some memory. Images will be fetched again when loading EPG next time.
     */
    fun clearEPGImageCache() {
        mChannelImageCache.clear()
    }

    private inner class OnGestureListener : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {

            // This is absolute coordinate on screen not taking scroll into account.
            val x = e.x.toInt()
            val y = e.y.toInt()

            // Adding scroll to clicked coordinate
            val scrollX = scrollX + x
            val scrollY = scrollY + y
            val channelPosition = getChannelPosition(scrollY)
            if (channelPosition != -1 && mClickListener != null) {
                if (calculateResetButtonHitArea().contains(scrollX, scrollY)) {
                    // Reset button clicked
                    mClickListener!!.onResetButtonClicked()
                } else if (calculateChannelsHitArea().contains(x, y)) {
                    // Channel area is clicked
                    mClickListener!!.onChannelClicked(
                        channelPosition,
                        epgData?.getChannel(channelPosition)
                    )
                } else if (calculateProgramsHitArea().contains(x, y)) {
                    // Event area is clicked
                    val programPosition = getProgramPosition(
                        channelPosition,
                        getTimeFrom(getScrollX() + x - calculateProgramsHitArea().left)
                    )
                    if (programPosition != -1) {
                        mClickListener!!.onEventClicked(
                            channelPosition,
                            programPosition,
                            epgData?.getEvent(channelPosition, programPosition)
                        )
                    }
                }
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            var dx = distanceX.toInt()
            var dy = distanceY.toInt()
            val x = scrollX
            val y = scrollY


            // Avoid over scrolling
            if (x + dx < 0) {
                dx = 0 - x
            }
            if (y + dy < 0) {
                dy = 0 - y
            }
            if (x + dx > mMaxHorizontalScroll) {
                dx = mMaxHorizontalScroll - x
            }
            if (y + dy > mMaxVerticalScroll) {
                dy = mMaxVerticalScroll - y
            }
            scrollBy(dx, dy)
            return true
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent,
            vX: Float, vY: Float
        ): Boolean {
            mScroller.fling(
                scrollX, scrollY, -vX.toInt(),
                -vY.toInt(), 0, mMaxHorizontalScroll, 0, mMaxVerticalScroll
            )
            redraw()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            if (!mScroller.isFinished) {
                mScroller.forceFinished(true)
                return true
            }
            return true
        }
    }

    companion object {
        const val DAYS_BACK_MILLIS = 3 * 24 * 60 * 60 * 1000 // 3 days
        const val DAYS_FORWARD_MILLIS = 3 * 24 * 60 * 60 * 1000 // 3 days
        const val HOURS_IN_VIEWPORT_MILLIS = 2 * 60 * 60 * 1000 // 2 hours
        const val TIME_LABEL_SPACING_MILLIS = 30 * 60 * 1000 // 30 minutes
    }
}
