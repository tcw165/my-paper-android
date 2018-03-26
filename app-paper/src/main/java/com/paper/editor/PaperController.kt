// Copyright Feb 2018-present boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.editor

import android.graphics.PointF
import android.os.Looper
import com.cardinalblue.gesture.MyMotionEvent
import com.paper.editor.data.GestureRecord
import com.paper.editor.view.ICanvasView
import com.paper.editor.view.IScrapLifecycleListener
import com.paper.editor.view.IScrapView
import com.paper.editor.view.SimpleGestureListener
import com.paper.shared.model.PaperModel
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.sketch.SketchModel
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class PaperController(private val mUiScheduler: Scheduler,
                      private val mWorkerScheduler: Scheduler)
    : SimpleGestureListener(),
      IScrapLifecycleListener {

    // View.
    private var mCanvasView: ICanvasView? = null

    // Model.
    private lateinit var mModel: PaperModel

    // Sub controllers.
    private val mControllers: HashMap<UUID, IScrapController> = hashMapOf()

    // Gesture detector.
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // Disposables
    private val mDisposablesOnBind = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    fun bindView(view: ICanvasView) {
        if (mCanvasView != null) throw IllegalStateException("Already bind to a view")

        ensureMainThread()

        mCanvasView = view

        mDisposablesOnBind.add(
            view.onLayoutFinished()
                .take(1)
                .observeOn(mUiScheduler)
                .subscribe {
                    // Inflate scraps.
                    mModel.scraps.forEach { scrap ->
                        view.addScrapView(scrap)
                    }
                })

        // TODO: Encapsulate the inflation with a custom Observable.
        view.setScrapLifecycleListener(this@PaperController)
        view.setGestureListener(this@PaperController)
        view.setCanvasSize(mModel.width, mModel.height)
    }

    fun unbindView() {
        if (mCanvasView == null) throw IllegalStateException("No view to unbind")

        ensureMainThread()

        mDisposablesOnBind.clear()

        mCanvasView?.setScrapLifecycleListener(null)
        mCanvasView?.setGestureListener(null)
        mCanvasView = null

        // Unbind views from sub-controllers
        recycleControllers()
    }

    private fun recycleControllers() {
        mControllers.values.forEach { controller ->
            controller.unbindView()
        }
        mControllers.clear()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Paper things ///////////////////////////////////////////////////////////

    fun setPaper(model: PaperModel) {
        ensureMainThread()

        recycleControllers()

        mModel = model
        // Create the scrap controller.
        mModel.scraps.forEach { scrap ->
            val controller = ScrapController(mUiScheduler,
                                             mWorkerScheduler)

            // Pass model reference to the controller.
            controller.loadScrap(scrap)

            // Add controller to the lookup table.
            mControllers[scrap.uuid] = controller
        }
    }

    fun getPaper(): PaperModel {
        return mModel
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scrap lifecycle ////////////////////////////////////////////////////////

    override fun onAttachToCanvas(view: IScrapView) {
        val controller = mControllers[view.getScrapId()]

        controller!!.bindView(view)
    }

    override fun onDetachFromCanvas(view: IScrapView) {
        val controller = mControllers[view.getScrapId()]

        controller!!.unbindView()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

    override fun onActionBegin(event: MyMotionEvent,
                               target: Any?,
                               context: Any?) {
        // Clear the gesture history.
        mGestureHistory.clear()
    }

    override fun onActionEnd(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        // DO NOTHING.
    }

    override fun onDragBegin(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        // Document gesture.
        mGestureHistory.add(GestureRecord.DRAG)

        // If PINCH is ever present, skip drawing sketch.
        if (mGestureHistory.contains(GestureRecord.PINCH)) return

        val x = event.downFocusX
        val y = event.downFocusY

        // Notify view.
        mCanvasView!!.startDrawStroke(x, y)
    }

    override fun onDrag(event: MyMotionEvent,
                        target: Any?,
                        context: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        // If PINCH is ever present, skip drawing sketch.
        if (mGestureHistory.contains(GestureRecord.PINCH)) return

        val x = event.downFocusX
        val y = event.downFocusY

        // Notify view.
        mCanvasView!!.onDrawStroke(x, y)
    }

    override fun onDragEnd(event: MyMotionEvent,
                           target: Any?,
                           context: Any?,
                           startPointer: PointF,
                           stopPointer: PointF) {
        // If PINCH is ever present, skip drawing sketch.
        if (mGestureHistory.contains(GestureRecord.PINCH)) return

        // Notify view.
        val stroke = mCanvasView!!.stopDrawStroke()

        // Commit the temporary scrap.
        val strokeBoundLeft = stroke.bound.left
        val strokeBoundTop = stroke.bound.top
        stroke.offset(-strokeBoundLeft, -strokeBoundTop)

        val sketch = SketchModel()
        sketch.addStroke(stroke)
        val scrap = ScrapModel()
        scrap.x = strokeBoundLeft
        scrap.y = strokeBoundTop
        scrap.sketch = sketch

        mModel.scraps.add(scrap)

        // Create controller.
        val controller = ScrapController(mUiScheduler, mWorkerScheduler)
        controller.loadScrap(scrap)
        mControllers[scrap.uuid] = controller

        // Inflate scraps.
        mCanvasView?.addScrapView(scrap)
    }

    override fun onPinchBegin(event: MyMotionEvent,
                              target: Any?,
                              context: Any?,
                              startPointers: Array<PointF>) {
        // Document gesture.
        mGestureHistory.add(GestureRecord.PINCH)

        mCanvasView?.startUpdateViewport()
    }

    override fun onPinch(event: MyMotionEvent,
                         target: Any?,
                         context: Any?,
                         startPointers: Array<PointF>,
                         stopPointers: Array<PointF>) {
        mCanvasView?.onUpdateViewport(startPointers, stopPointers)
    }

    override fun onPinchEnd(event: MyMotionEvent,
                            target: Any?,
                            context: Any?,
                            startPointers: Array<PointF>,
                            stopPointers: Array<PointF>) {
        mCanvasView?.stopUpdateViewport()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException("Not in MAIN thread")
        }
    }
}
