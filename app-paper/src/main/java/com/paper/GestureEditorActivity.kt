package com.paper

import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import com.my.widget.gesture.IGestureListener
import com.my.widget.gesture.MyGestureDetector
import com.my.widget.gesture.MyMotionEvent
import java.util.*

class GestureEditorActivity : AppCompatActivity(),
                              IGestureListener {

    private val mLog: MutableList<String> = mutableListOf()

    private val mBtnClearLog: ImageView by lazy {
        findViewById(R.id.btn_clear) as ImageView
    }
    private val mTxtLog: TextView by lazy {
        findViewById(R.id.text_gesture_test) as TextView
    }

    private val mGestureDetector: MyGestureDetector by lazy {
        MyGestureDetector(this@GestureEditorActivity,
                          this,
                          resources.getDimension(R.dimen.touch_slop),
                          resources.getDimension(R.dimen.tap_slop),
                          resources.getDimension(R.dimen.fling_min_vec),
                          resources.getDimension(R.dimen.fling_max_vec))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_gesture_editor)

        RxView.clicks(mBtnClearLog)
            .subscribe { _ ->
                clearLog()
            }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean =
        mGestureDetector.onTouchEvent(event, null, null)

    // GestureListener ----------------------------------------------------->

    override fun onActionBegin() {
        printLog("--------------")
        printLog("⬇onActionBegin")
    }

    override fun onActionEnd() {
        printLog("⬆onActionEnd")
    }

    override fun onSingleTap(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
        printLog(String.format(Locale.ENGLISH, "\uD83D\uDD95 x%d onSingleTap", 1))
    }

    override fun onDoubleTap(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
        printLog(String.format(Locale.ENGLISH, "\uD83D\uDD95 x%d onDoubleTap", 2))
    }

    override fun onMoreTap(event: MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?,
                           tapCount: Int) {
        printLog(String.format(Locale.ENGLISH, "\uD83D\uDD95 x%d onMoreTap", tapCount))
    }

    override fun onLongTap(event: MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?) {
        printLog(String.format(Locale.ENGLISH, "\uD83D\uDD95 x%d onLongTap", 1))
    }

    override fun onLongPress(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
        printLog("\uD83D\uDD50 onLongPress")
    }

    override fun onDragBegin(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?): Boolean {
        printLog("✍️ onDragBegin")
        return true
    }

    override fun onDrag(event: MyMotionEvent,
                        touchingObject: Any?,
                        touchingContext: Any?,
                        startPointerInCanvas: PointF,
                        stopPointerInCanvas: PointF) {
        // DO NOTHING.
        printLog("✍️ onDrag")
    }

    override fun onDragEnd(event: MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?,
                           startPointerInCanvas: PointF,
                           stopPointerInCanvas: PointF) {
        printLog("✍️ onDragEnd")
    }

    override fun onDragFling(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchContext: Any?,
                             startPointerInCanvas: PointF,
                             stopPointerInCanvas: PointF,
                             velocityX: Float,
                             velocityY: Float): Boolean {
        printLog("✍ \uD83C\uDFBC onDragFling")
        return true
    }

    override fun onPinchBegin(event: MyMotionEvent,
                              touchingObject: Any?,
                              touchContext: Any?,
                              startPointers: Array<PointF>): Boolean {
        printLog("\uD83D\uDD0D onPinchBegin")
        return true
    }

    override fun onPinch(event: MyMotionEvent,
                         touchingObject: Any?,
                         touchContext: Any?,
                         startPointersInCanvas: Array<PointF>,
                         stopPointersInCanvas: Array<PointF>) {
        printLog("\uD83D\uDD0D onPinch")
    }

    override fun onPinchFling(event: MyMotionEvent,
                              touchingObject: Any?,
                              touchContext: Any?) {
        printLog("\uD83D\uDD0D onPinchFling")
    }

    override fun onPinchEnd(event: MyMotionEvent,
                            touchingObject: Any?,
                            touchContext: Any?,
                            startPointersInCanvas: Array<PointF>,
                            stopPointersInCanvas: Array<PointF>) {
        printLog("\uD83D\uDD0D onPinchEnd")
    }

    // GestureListener <- end -----------------------------------------------

    private fun printLog(msg: String) {
        mLog.add(msg)
        while (mLog.size > 32) {
            mLog.removeAt(0)
        }

        val builder = StringBuilder()
        mLog.forEach { line ->
            builder.append(line)
            builder.append(System.lineSeparator())
        }

        mTxtLog.text = builder.toString()
    }

    private fun clearLog() {
        mLog.clear()
        mTxtLog.text = getString(R.string.tap_anywhere_to_start)
    }
}