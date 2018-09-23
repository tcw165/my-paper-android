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

package com.paper.view.canvas

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.os.Looper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.ViewConfiguration
import android.widget.Toast
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.rx.*
import com.paper.AppConst
import com.paper.R
import com.paper.domain.DomainConst
import com.paper.domain.data.GestureRecord
import com.paper.domain.ui_event.*
import com.paper.domain.util.ProfilerUtils
import com.paper.TransformUtils
import com.paper.domain.ui.IEditorWidget
import com.paper.domain.ui.IBaseScrapWidget
import com.paper.model.IPreferenceServiceProvider
import com.paper.model.ModelConst
import com.paper.model.Point
import com.paper.model.Rect
import com.paper.model.repository.IBitmapRepository
import com.paper.model.sketch.SVGStyle
import com.paper.observables.AddFileToMediaStoreMaybe
import com.paper.observables.WriteBitmapToFileMaybe
import com.paper.services.IContextProvider
import com.paper.view.IWidgetView
import com.paper.view.with
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PaperCanvasView : TextureView,
                        TextureView.SurfaceTextureListener,
                        IWidgetView<IEditorWidget>,
                        IPaperContext,
                        IParentView,
                        IWriteThumbnailFileCanvasView,
                        IWriteHDResolutionFileCanvasView {

    // Scraps.
    private val mScrapViews = mutableListOf<IScrapView>()

    // Widget.
    private lateinit var mWidget: IEditorWidget
    private val mDisposables = CompositeDisposable()

    // Dirty flags
    private var mIsNew = true
    private val mDirtyFlag = CanvasViewDirtyFlag(
        flag = CanvasViewDirtyFlag.VIEW_MEASURING.or(
            CanvasViewDirtyFlag.VIEW_PREPARING_SURFACE).or(
            CanvasViewDirtyFlag.VIEW_PREPARING_CONFIG))

    // Preferences
    private val mPrefs by lazy { (context.applicationContext as IPreferenceServiceProvider).preference }

    // Temporary utils
    private val mTmpPoint = FloatArray(2)
    private val mTmpMatrix = Matrix()
    private val mTmpMatrixInverse = Matrix()
    private val mTmpMatrixStart = Matrix()

    /**
     * A util for getting x, y, scaleX, scaleY, and
     * rotationInDegrees from a [Matrix]
     */
    private val mTransformHelper = TransformUtils()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr) {
        mGridPaint.color = Color.LTGRAY
        mGridPaint.style = Paint.Style.STROKE
        mGridPaint.strokeWidth = 2f * mOneDp

        mBitmapPaint.isAntiAlias = true

        mEraserPaint.style = Paint.Style.FILL
        mEraserPaint.color = Color.WHITE
        mEraserPaint.xfermode = mEraserMode

        surfaceTextureListener = this
    }

    override fun bindWidget(widget: IEditorWidget) {
        ensureMainThread()
        ensureNoLeakingSubscription()

        mWidget = widget

        // Debug
        mDisposables.add(
            widget.observeDebugMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                })
        mDisposables.add(
            onReadyToDraw()
                .subscribe { drawReady ->
                    println("${AppConst.TAG}: Draw ready=$drawReady")
                })
        mDisposables.add(
            onReadyToInteract()
                .subscribe { interactionReady ->
                    println("${AppConst.TAG}: Interaction ready=$interactionReady")
                })

        // Preference
        mDisposables.add(
            onConfigReady()
                .subscribe {
                    mDirtyFlag.markNotDirty(CanvasViewDirtyFlag.VIEW_PREPARING_CONFIG)
                })

        // View-port
        mDisposables.add(
            onUpdateViewPortScale()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scale ->
                    // To get the right scaled pen size
                    widget.setViewPortScale(scale)
                })

        // Drawing
        mDisposables.add(
            onReadyToDraw()
                .switchMap { readyToDraw ->
                    if (readyToDraw) {
                        widget.observeCanvasSize()
                            .observeOn(AndroidSchedulers.mainThread())
                            .switchMap { size ->
                                updateLayoutOrCanvas(size.width, size.height)

                                Observable.merge(listOf(
                                    // Initialization from view-model
                                    // TODO: Remove it
                                    widget.onDrawSVG(replayAll = true),
                                    mInvalidateCanvasSignal,
                                    // Consume the [GestureEvent] and produce [CanvasEvent]
                                    GestureEventObservable(mGestureDetector).compose(touchEventToCanvasEvent()),
                                    // Other canvas event sources
                                    Observable.merge(mOtherCanvasEventSources),
                                    // Anti-aliasing drawing signal
                                    onAntiAliasingDraw()))
                                    .compose(consumeCanvasEvent())
                            }
                    } else {
                        Observable.empty()
                    }
                }
                .subscribe())

        // Add or remove scraps
        mDisposables.add(
            onReadyToDraw()
                .switchMap { ready ->
                    if (ready) {
                        widget.observeScraps()
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    addScrap(scrapWidget)
                })
        mDisposables.add(
            onReadyToDraw()
                .switchMap { ready ->
                    if (ready) {
                        widget.onRemoveScrap()
                            .subscribeOn(AndroidSchedulers.mainThread())
                    } else {
                        Observable.empty()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    removeScrap(scrapWidget)
                })
    }

    override fun unbindWidget() {
        mDisposables.clear()

        synchronized(mLock) {
            mSurface?.release()
            mSurface = null
        }

        mScrapViews.forEach { scrapView ->
            scrapView.unbindWidget()
        }
    }

    override fun onMeasure(widthSpec: Int,
                           heightSpec: Int) {
        println("${AppConst.TAG}: PaperCanvasView # onMeasure()")

        mDirtyFlag.markDirty(CanvasViewDirtyFlag.VIEW_MEASURING)

        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        println("${AppConst.TAG}: PaperCanvasView # onLayout(changed=$changed)")
        super.onLayout(changed, left, top, right, bottom)

        mDirtyFlag.markNotDirty(CanvasViewDirtyFlag.VIEW_MEASURING)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture,
                                             width: Int,
                                             height: Int) {
        mDirtyFlag.markDirty(CanvasViewDirtyFlag.VIEW_PREPARING_SURFACE)
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // DO NOTHING
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        mDirtyFlag.markDirty(CanvasViewDirtyFlag.VIEW_PREPARING_SURFACE)
        return true
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture,
                                           width: Int,
                                           height: Int) {
        mDirtyFlag.markNotDirty(CanvasViewDirtyFlag.VIEW_PREPARING_SURFACE)

        synchronized(mLock) {
            mSurface = Surface(surfaceTexture)
        }
    }

    // Preference //////////////////////////////////////////////////////////////

    private fun onConfigReady(): Observable<Any> {
        return Observables
            .combineLatest(
                mPrefs.getBoolean(context.resources.getString(R.string.prefs_show_path_joints), false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { enabled ->
                        mIfShowPathJoints = enabled
                        println("${AppConst.TAG}: show path joints=$mIfShowPathJoints")
                    },
                mPrefs.getString(context.resources.getString(R.string.prefs_path_interpolator),
                                 mPathInterpolatorID)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { interpolator ->
                        mPathInterpolatorID = interpolator
                        println("${AppConst.TAG}: path interpolator ID=$mPathInterpolatorID")
                    },
                mPrefs.getInt(context.resources.getString(R.string.prefs_min_path_segment_factor),
                              mMinPathSegment.toInt())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { minPathSegment ->
                        mMinPathSegment = minPathSegment.toFloat() * getOneDp()
                        println("${AppConst.TAG}: min path segment=$mMinPathSegment")
                    })
            .map { Any() }
    }

    // Add / Remove Scraps /////////////////////////////////////////////////////

    private fun addScrap(widget: IBaseScrapWidget) {
        ensureMainThread()

        val scrapView = ScrapView(renderScheduler = mRenderingScheduler)

        scrapView.setPaperContext(this)
        scrapView.setParent(this)

        mScrapViews.add(scrapView)

        scrapView.bindWidget(widget)

        invalidate()
    }

    private fun removeScrap(widget: IBaseScrapWidget) {
        ensureMainThread()

        val scrapView = mScrapViews.firstOrNull { it == widget }
                        ?: throw NoSuchElementException("Cannot find the widget")

        scrapView.unbindWidget()
        mScrapViews.remove(scrapView)

        invalidate()
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private val mRenderingScheduler = SingleScheduler()

    // Lock
    private val mLock = Any()

    private var mSurface: Surface? = null
    private val mDirtyRect = android.graphics.Rect()

    /**
     * A signal of requesting the anti-aliasing drawing.
     */
    private val mAntiAliasingSignal = PublishSubject.create<Boolean>()

    /**
     * Model canvas size.
     */
    private var mMSize = Rect()
        set(value) {
            field.set(value)
        }
    /**
     * Scale factor from Model size to View size.
     */
    private var mScaleM2V = Float.NaN

    /**
     * The matrix used for mapping a point from the canvas world to view port
     * world in the View perspective.
     *
     * @sample
     * .---------------.
     * |               | ----> View canvas
     * |       .---.   |
     * |       | V | --------> View port
     * |       | P |   |
     * |       '---'   |
     * '---------------'
     */
    private val mCanvasMatrix = Matrix()
    /**
     * The inverse matrix of [mCanvasMatrixInverse], which is used to mapping a
     * point from view port world to canvas world in the View perspective.
     */
    private val mCanvasMatrixInverse = Matrix()
    private var mCanvasMatrixDirty = false

    // Rendering resource.
    private val mOneDp by lazy { context.resources.getDimension(R.dimen.one_dp) }
    private val mParentTransforms = Stack<Matrix>()

    private val mBitmapPaint = Paint()
    private val mEraserPaint = Paint()
    private val mDrawMode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    private val mEraserMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    // Background & grids
    private val mGridPaint = Paint()

//    // Temporary strokes
//    private val mStrokeDrawables = CopyOnWriteArrayList<SVGDrawable>()

    private fun updateLayoutOrCanvas(canvasWidth: Float,
                                     canvasHeight: Float) {
        // The maximum view port, a rectangle as the same width over
        // height ratio and it just fits in the canvas rectangle as
        // follow:
        // .-------.-----.-----.
        // |       |/////|     | <-- model canvas size
        // |       |/////| <-------- view port observed in model world
        // |       |/////|     |
        // |       |/////|     |
        // '-------'-----'-----'
        //
        // The minimum view port, a rectangle N times as small as the
        // max view port.
        // .-------------------.
        // |         .--.      | <-- model canvas size
        // |         '--'  <-------- min view port
        // |                   |
        // |                   |
        // '-------------------'
        val spaceWidth = this.width - ViewCompat.getPaddingStart(this) - ViewCompat.getPaddingEnd(this)
        val spaceHeight = this.height - paddingTop - paddingBottom
        val maxScale = Math.min(canvasWidth / spaceWidth,
                                canvasHeight / spaceHeight)
        val minScale = maxScale / DomainConst.VIEW_PORT_MIN_SCALE
        val scaleM2V = 1f / maxScale
        mViewPortMax.set(0f, 0f, maxScale * spaceWidth, maxScale * spaceHeight)
        mViewPortMin.set(0f, 0f, minScale * spaceWidth, minScale * spaceHeight)
        mViewPortBase.set(mViewPortMax)

        // Hold canvas size.
        mMSize = Rect(0f, 0f, canvasWidth, canvasHeight)

        // Initially the model-to-view scale is derived by the scale
        // from min view port boundary to the view boundary.
        // Check out the figure above :D
        mScaleM2V = scaleM2V

        // Determine the default view-port (makes sense when view
        // layout is changed).
        resetViewPort()
    }

    private fun onReadyToDraw(): Observable<Boolean> {
        return mDirtyFlag
            .onUpdate(CanvasViewDirtyFlag.VIEW_PREPARING_CONFIG,
                      CanvasViewDirtyFlag.VIEW_MEASURING,
                      CanvasViewDirtyFlag.VIEW_PREPARING_SURFACE)
            .map { event ->
                event.flag == 0
            }
            .debounce(150, TimeUnit.MILLISECONDS)
    }

    private fun onReadyToInteract(): Observable<Boolean> {
        return mDirtyFlag
            .onUpdate()
            .map { event ->
                event.flag == 0
            }
            .debounce(150, TimeUnit.MILLISECONDS)
    }

    private fun getPaintMode(penType: SVGStyle): PorterDuffXfermode {
        return if (penType == SVGStyle.ERASER) mEraserMode else mDrawMode
    }

    /**
     * Apply the padding transform to the canvas before processing the given
     * lambda.
     */
    private inline fun<T> Canvas.withPadding(lambda: (canvas: Canvas) -> T):T {
        return with {
            // View might have padding, if so we need to shift canvas to show
            // padding on the screen.
            translate(ViewCompat.getPaddingStart(this@PaperCanvasView).toFloat(),
                      paddingTop.toFloat())
            lambda(this)
        }
    }

    private fun dispatchDrawScraps(canvas: Canvas,
                                   scrapViews: List<IScrapView>,
                                   ifSharpenDrawing: Boolean) {
        // Hold canvas matrix.
        mTmpMatrix.set(mCanvasMatrix)

        // Prepare the transform stack for later sharp rendering.
        if (mParentTransforms.size != 1 ||
            mParentTransforms[0] != mCanvasMatrix) {
            mParentTransforms.clear()
            mParentTransforms.push(mCanvasMatrix)
        }

        scrapViews.forEach { scrapView ->
            scrapView.dispatchDraw(canvas, mParentTransforms, ifSharpenDrawing)
        }

        // Ensure no scraps modify the canvas matrix.
        if (mTmpMatrix != mCanvasMatrix) {
            throw IllegalStateException("Canvas matrix is changed")
        }
    }

    private val mInvalidateCanvasSignal = PublishSubject.create<CanvasEvent>().toSerialized()

    override fun invalidateCanvas() {
        mInvalidateCanvasSignal.onNext(InvalidationEvent)
    }

    //    override fun onDraw(canvas: Canvas) {
//        if (!mDrawReadySignal.value!!) return
//
//        // Scale from model to view.
//        val scaleM2V = mScaleM2V
//        // Scale contributed by view port.
//        val mw = mMSize.width
//        val mh = mMSize.height
//        val vw = scaleM2V * mw
//        val vh = scaleM2V * mh
//
//        // Calculate the view port matrix.
//        computeCanvasMatrix(scaleM2V)
//
//        // Layers rendering ///////////////////////////////////////////////////
//
//        // Background layer
//        canvas.withPadding { c ->
//            c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
//
//            // Extract the transform from the canvas matrix.
//            mTransformHelper.getValues(mCanvasMatrix)
//            val tx = mTransformHelper.x
//            val ty = mTransformHelper.y
//            val scaleVP = mTransformHelper.scaleX
//
//            // Manually calculate position and size of the background cross/grids
//            // so that they keep sharp!
//            drawBackground(c, vw, vh, tx, ty, scaleVP)
//        }
//
//        // Layers blending ////////////////////////////////////////////////////
//
//        mMergedBitmap?.eraseColor(Color.TRANSPARENT)
//
//        // Print the thumbnail Bitmap to the merged layer
//        mMergedCanvas.withPadding { c ->
//            c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
//            c.concat(mCanvasMatrix)
//            c.scale(mScaleThumb, mScaleThumb)
//            c.drawBitmap(mThumbBitmap, 0f, 0f, mBitmapPaint)
//        }
//
//        // Print the anti-aliasing Bitmap to the merged layer
//        mMergedCanvas.withPadding { c ->
//            c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
//            mSceneBuffer.getCurrentScene().print(c)
//        }
//
//        // Print the merged layer to view canvas
//        canvas.with { c ->
//            c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
//            c.drawBitmap(mMergedBitmap, 0f, 0f, mBitmapPaint)
//        }
//    }

    private fun consumeCanvasEvent(): ObservableTransformer<CanvasEvent, Unit> {
        return ObservableTransformer { upstream ->
            upstream
                .observeOn(mRenderingScheduler)
                // To drawable (must do) and invalidation event
                .flatMap { event ->
                    when (event) {
                        is ViewPortEvent -> {
                            consumeViewPortEvent(event)

                            Observable.just(NullCanvasEvent)
                        }
//                        is StartSketchEvent -> {
//                            val nx = event.point.x
//                            val ny = event.point.y
//                            val (x, y) = toViewWorld(nx, ny)
//
//                            val drawable = createSvgDrawable(event)
//                            drawable.moveTo(Point(x, y, event.point.time))
//
//                            mStrokeDrawables.add(drawable)
//
//                            isHashDirty = true
//
//                            Observable.just(InvalidationEvent())
//                        }
//                        is DoSketchEvent -> {
//                            val nx = event.point.x
//                            val ny = event.point.y
//                            val (x, y) = toViewWorld(nx, ny)
//
//                            val drawable = mStrokeDrawables.last()
//                            drawable.lineTo(Point(x, y, event.point.time))
//
//                            isHashDirty = true
//
//                            Observable.just(InvalidationEvent())
//                        }
//                        is StopSketchEvent -> {
//                            val drawable = mStrokeDrawables.last()
//                            drawable.close()
//
//                            Observable.just(InvalidationEvent())
//                        }
//                        is AddSvgEvent -> {
//                            if (-1 == mStrokeDrawables.indexOfFirst { d -> d.id == event.svgID }) {
//                                val drawable = createSvgDrawable(event)
//                                mStrokeDrawables.add(drawable)
//
//                                isHashDirty = true
//
//                                Observable.just(InvalidationEvent())
//                            } else {
//                                Observable.just(NullCanvasEvent())
//                            }
//                        }
//                        is RemoveSvgEvent -> {
//                            val i = mStrokeDrawables.indexOfFirst { d ->
//                                d.id == event.svgID
//                            }
//                            if (i >= 0) {
//                                mStrokeDrawables.removeAt(i)
//
//                                // Invalidate drawables
//                                mStrokeDrawables.forEach { d -> d.markUndrew() }
//
//                                isHashDirty = true
//
//                                Observable.just(
//                                    EraseCanvasEvent(),
//                                    InvalidationEvent())
//                            } else {
//                                Observable.just(NullCanvasEvent())
//                            }
//                        }
                        else -> Observable.just(event)
                    }
                }
                .observeOn(mRenderingScheduler)
                // State reducing
                .scan { prev: CanvasEvent, now: CanvasEvent ->
                    if (prev is InitializationBeginEvent ||
                        (prev is InitializationDoingEvent &&
                         now !is InitializationEndEvent)) {
                        // Transform any events in between init-begin and init-end
                        // to init-doing events.
                        // This is only happening in the initialization process
                        // e.g.
                        // Given  [init begin, _whatever_, ..., init end]
                        // Return [init begin, init doing, ..., init end]
                        //
                        // It's important to use the same observable pattern to
                        // initialize the stroke without slowing down by the
                        // frequent rendering requests.
                        InitializationDoingEvent
                    } else {
                        if (now !is InitializationEndEvent) {
                            mIsNew = false
                        }

                        // Given  [..., init end, foo]
                        // Return [..., init end, foo]
                        now
                    }
                }
                .observeOn(mRenderingScheduler)
                .map { event ->
                    when (event) {
                        is InitializationBeginEvent -> {
                            // Mark interaction disabled
                            mDirtyFlag.markDirty(CanvasViewDirtyFlag.VIEW_DRAWING)
                        }
                        is InitializationEndEvent,
                        is InvalidationEvent -> {
                            // First try to load Bitmap from file;
                            // secondly try to redraw
//                            try {
//                                // Load file
//                                val hash = hashCode()
//                                ProfilerUtils.with("load thumbnail") {
//                                    val bmp = mBitmapRepo?.getBitmap(hash)?.blockingGet()
//                                              ?: throw NullPointerException()
//                                    mThumbCanvas.with { c ->
//                                        c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
//                                        c.drawBitmap(bmp, 0f, 0f, mBitmapPaint)
//                                    }
//                                    bmp.recycle()
//
//                                    println("${AppConst.TAG}: Found thumbnail cache, so skip drawing")
//                                }
//                            } catch (err: Throwable) {
//                                // Redraw
//                                ProfilerUtils.with("draw thumbnail") {
//                                    // Draw sketch and scraps on thumbnail Bitmap
//                                    // TODO: Both scraps and sketch need to explicitly define the z-order
//                                    // TODO: so that the whiteboard knows how to render them in the correct
//                                    // TODO: order.
//                                    canvas.with { c ->
//                                        c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
//                                        c.scale(1f / mScaleThumb, 1f / mScaleThumb)
//
//                                        var dirty = false
//                                        //dispatchDrawScraps(canvas = c,
//                                        //                   scrapViews = mScrapViews,
//                                        //                   ifSharpenDrawing = false)
//                                        // Draw the strokes on the thumbnail canvas
//                                        mStrokeDrawables.forEach { drawable ->
//                                            dirty = dirty || drawable.isSomethingToDraw()
//                                            drawable.draw(canvas = c)
//                                        }
//                                    }
//
//                                    println("${AppConst.TAG}: Not found thumbnail cache, so draw it again!")
//                                }
//                            }

                            synchronized(mLock) {
                                val surface = mSurface!!
                                mDirtyRect.set(0, 0, width, height)
                                // Get available canvas
                                val canvas = surface.lockCanvas(mDirtyRect) ?: throw IllegalStateException()

                                canvas.with { c ->
                                    c.clipRect(0, 0, width, height)
//                                    c.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
                                    c.drawRGB(255, 255, 255)
                                }

                                // Draw sketch on view-port Bitmap
                                ProfilerUtils.with("draw view-port") {
                                    canvas.with { c ->
                                        computeCanvasMatrix(mScaleM2V)

//                                        c.clipRect(0, 0, width, height)
//                                        c.concat(mCanvasMatrix)
//
//                                        mStrokeDrawables.forEach { d ->
//                                            d.draw(canvas = c)
//                                        }
                                        dispatchDrawScraps(c, mScrapViews, false)
                                    }
                                }

//                                // By marking drawables not dirty, the drawable's
//                                // cache is renewed.
//                                mStrokeDrawables.forEach { d ->
//                                    d.markUndrew()
////                                    d.markAllDrew()
//                                }

                                // Submit canvas
                                surface.unlockCanvasAndPost(canvas)
                            }

                            // Mark interaction enabled
                            if (event is InitializationEndEvent) {
                                mDirtyFlag.markNotDirty(CanvasViewDirtyFlag.VIEW_DRAWING)
                            }
                        }
                        is EraseCanvasEvent -> {
                            synchronized(mLock) {
                                val surface = mSurface!!
                                mDirtyRect.set(0, 0, width, height)
                                // Get available canvas
                                val canvas = surface.lockCanvas(mDirtyRect) ?: throw IllegalStateException()
                                canvas.with { c ->
//                                    c.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
                                    c.drawRGB(255, 255, 255)
                                }
                                // Submit canvas
                                surface.unlockCanvasAndPost(canvas)
                            }
                        }
                    }
                }
        }
    }

    private fun onAntiAliasingDraw(): Observable<CanvasEvent> {
        return mAntiAliasingSignal
            .switchMap { doAntiAliasingDraw ->
                if (doAntiAliasingDraw) {
                    Observable.just(InvalidationEvent)
                } else {
                    Observable.empty<CanvasEvent>()
                }
            }
    }

    private var mBitmapRepo: IBitmapRepository? = null

    override fun injectBitmapRepository(repo: IBitmapRepository) {
        mBitmapRepo = repo
    }

    private fun getThumbnailBitmap(): Single<Bitmap> {
        return Single
            .fromCallable {
                // Get Bitmap from the texture
                bitmap
            }
            .subscribeOn(Schedulers.io())
    }

    /**
     * Write the thumbnail Bitmap to a file maintained by the Bitmap repository.
     */
    override fun writeThumbFileToBitmapRepository(): Maybe<Triple<File, Int, Int>> {
        return if (mIsNew) {
            Maybe.empty()
        } else {
            val hash = hashCode()
            getThumbnailBitmap()
                .flatMap { bmp ->
                    mBitmapRepo
                        ?.putBitmap(hash, bmp)
                        ?.map { file ->
                            val fileAndSize = Triple(file, bmp.width, bmp.height)

                            // Recycle the Bitmap
                            bmp.recycle()

                            fileAndSize
                        }
                    ?: Single.never<Triple<File, Int, Int>>()
                }
                .toMaybe()
        }
    }

    override fun writeFileToSystemMediaStore(): Maybe<String> {
        // Create the file snapshot
        val pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val bmpFile = File(pictureDir, "whiteboard-${SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())}.png")

        // TODO: Check if the canvas is pixel wisely empty
        return Maybe
            .fromCallable {
                val vw = mScaleM2V * mMSize.width
                val vh = mScaleM2V * mMSize.height
                val scale = Math.max(DomainConst.BASE_HD_WIDTH / vw,
                                     DomainConst.BASE_HD_HEIGHT / vh)

                // FIXME: Update the rendering when there is no canvas boundary
                // Draw the canvas on HD Bitmap
                val bmp = Bitmap.createBitmap((scale * vw).toInt(),
                                              (scale * vh).toInt(),
                                              Bitmap.Config.ARGB_8888)
                // Set white as default background color
                bmp.eraseColor(Color.WHITE)
                val bmpCanvasMatrix = Matrix()
                bmpCanvasMatrix.postScale(scale, scale)
                val bmpCanvas = Canvas(bmp)
                bmpCanvas.with { c ->
//                    c.concat(bmpCanvasMatrix)
//                    mStrokeDrawables.forEach { d ->
//                        d.markUndrew()
//                        d.draw(c)
//                        d.markAllDrew()
//                    }
                    dispatchDrawScraps(c, mScrapViews, true)
                }

                bmp
            }
            .subscribeOn(Schedulers.io())
            .flatMap { bmp ->
                // Write Bitmap to a file
                WriteBitmapToFileMaybe(inputBitmap = bmp,
                                       outputFile = bmpFile,
                                       recycleBitmap = true)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        val viewContext = context
                        // Add the file to system media store
                        AddFileToMediaStoreMaybe(
                            contextProvider = object: IContextProvider {
                                override val context: Context?
                                    get() = if (isAttachedToWindow) viewContext else null
                            },
                            file = bmpFile)
                            .subscribeOn(Schedulers.io())
                            .map { uri -> uri.toString() }
                    }

            }
    }

    private fun verifyViewState() {
        if (!isAttachedToWindow) throw IllegalStateException()
    }

    private fun getScaledPenSize(event: CanvasEvent): Float {
        return when (event) {
            is StartSketchEvent -> {
                mapM2V(event.penSize)
            }
            is AddSvgEvent -> {
                mapM2V(event.penSize)
            }
            else -> throw IllegalArgumentException("Invalid event")
        }
    }

    // View port //////////////////////////////////////////////////////////////

    /**
     * The view-port boundary in the model world.
     */
    private var mViewPort = Rect()
        set(value) {
            field.set(value)

            mCanvasMatrixDirty = true

            // Notify any external observers
            mDrawViewPortSignal.onNext(DrawViewPortEvent(
                canvas = mMSize.copy(),
                viewPort = Rect(value.left,
                                value.top,
                                value.right,
                                value.bottom)))

            postInvalidate()
        }

    private val mViewPortStart = Rect()
    /**
     * Minimum size of [mViewPort].
     */
    private val mViewPortMin = Rect()
    /**
     * Maximum size of [mViewPort].
     */
    private val mViewPortMax = Rect()
    /**
     * The initial view port size that is used to compute the scale factor from
     * Model to View, which is [mScaleM2V].
     */
    private val mViewPortBase = Rect()
    /**
     * The signal for external observers.
     */
    private val mDrawViewPortSignal = BehaviorSubject.create<DrawViewPortEvent>()

    fun onDrawViewPort(): Observable<DrawViewPortEvent> {
        return mDrawViewPortSignal
    }

    private fun onUpdateViewPortScale(): Observable<Float> {
        return mDrawViewPortSignal
            .map { event ->
                event.viewPort.width / mViewPortBase.width
            }
    }

    private fun resetViewPort() {
        val defaultW = mViewPortMax.width
        val defaultH = mViewPortMax.height

        // Place the view port left in the model world.
        val viewPortX = 0f
        val viewPortY = 0f
        mViewPort = Rect(viewPortX, viewPortY,
                         viewPortX + defaultW,
                         viewPortY + defaultH)
    }

    /**
     * Compute the [mCanvasMatrix] given [mViewPort].
     *
     * @param scaleM2V The scale from model to view.
     */
    private fun computeCanvasMatrix(scaleM2V: Float) {
        if (mCanvasMatrixDirty) {
            // View port x
            val vx = mViewPort.left
            // View port y
            val vy = mViewPort.top
            // View port width
            val vw = mViewPort.width
            val scaleVP = mViewPortBase.width / vw

            mCanvasMatrix.reset()
            //        canvas width
            // .-------------------------.
            // |h         .---.          |
            // |e         | V | --------------> view port
            // |i         | P |          |
            // |g         '---'          | ---> model
            // |h                        |
            // |t                        |
            // '-------------------------'
            mCanvasMatrix.postScale(scaleVP,
                                    scaleVP)
            mCanvasMatrix.postTranslate(-scaleVP * scaleM2V * vx,
                                        -scaleVP * scaleM2V * vy)
            mCanvasMatrix.invert(mCanvasMatrixInverse)

            mCanvasMatrixDirty = false
        }
    }

    private var mSnapshotBitmap: Bitmap? = null

    private fun consumeViewPortEvent(event: ViewPortEvent) {
        ensureNotMainThread()

        when (event) {
            is ViewPortBeginUpdateEvent -> {
                // Cancel anti-aliasing drawing
                mAntiAliasingSignal.onNext(false)

                // Hold necessary starting states.
                mTmpMatrixStart.set(mCanvasMatrix)
                mViewPortStart.set(mViewPort)

                // Take snapshot for later transform
                synchronized(mLock) {
                    // TODO: Detect whether the canvas is pixel-wisely empty or not
                    if (hashCode() != AppConst.EMPTY_HASH) {
                        mSnapshotBitmap = bitmap
                    }
                }
            }
            is ViewPortOnUpdateEvent -> {
                val bound = constraintViewPort(
                    event.bound,
                    left = 0f,
                    top = 0f,
                    right = mMSize.width,
                    bottom = mMSize.height,
                    minWidth = mViewPortMin.width,
                    minHeight = mViewPortMin.height,
                    maxWidth = mViewPortMax.width,
                    maxHeight = mViewPortMax.height)

                // After applying the constraint, calculate the matrix for anti-aliasing
                // Bitmap
                val scaleVp = mViewPortBase.width / bound.width
                val vpDs = mViewPortStart.width / bound.width
                val vpDx = (mViewPortStart.left - bound.left) * mScaleM2V * scaleVp
                val vpDy = (mViewPortStart.top - bound.top) * mScaleM2V * scaleVp
                mTmpMatrix.reset()
                mTmpMatrix.postScale(vpDs, vpDs)
                mTmpMatrix.postTranslate(vpDx, vpDy)

                // Calculate the canvas matrix contributed by view-port boundary.
                mCanvasMatrixDirty = true
                computeCanvasMatrix(mScaleM2V)

                // Apply final view port boundary
                mViewPort = bound

                synchronized(mLock) {
                    mSnapshotBitmap?.let { bmp ->

                        val surface = mSurface!!

                        mDirtyRect.set(0, 0, width, height)
                        // Acquire canvas
                        val canvas = surface.lockCanvas(mDirtyRect)

                        // Draw snapshot with transform
                        canvas.with {
                            canvas.drawRGB(255, 255, 255)
                            canvas.concat(mTmpMatrix)
                            canvas.drawBitmap(bmp, 0f, 0f, mBitmapPaint)
                        }

                        // Enqueue canvas
                        surface.unlockCanvasAndPost(canvas)
                    }
                }
            }
            is ViewPortStopUpdateEvent -> {
                mTmpMatrixStart.reset()
                mTmpMatrix.reset()
                mTmpMatrixInverse.reset()

                // Recycle the snapshot
                synchronized(mLock) {
                    mSnapshotBitmap?.recycle()
                    mSnapshotBitmap = null
                }

                // Request anti-aliasing drawing
                mAntiAliasingSignal.onNext(true)
            }
        }
    }

    // TODO: Make the view-port code a component.
    private fun calculateViewPortBound(startPointers: Array<PointF>,
                                       stopPointers: Array<PointF>): Rect {
        // Compute new canvas matrix.
        val transform = TransformUtils.getTransformFromPointers(
            startPointers, stopPointers)
        val dx = transform[TransformUtils.DELTA_X]
        val dy = transform[TransformUtils.DELTA_Y]
        val dScale = transform[TransformUtils.DELTA_SCALE_X]
        val px = transform[TransformUtils.PIVOT_X]
        val py = transform[TransformUtils.PIVOT_Y]
        mTmpMatrix.set(mTmpMatrixStart)
        mTmpMatrix.postScale(dScale, dScale, px, py)
        mTmpMatrix.postTranslate(dx, dy)
        mTmpMatrix.invert(mTmpMatrixInverse)

        // Compute new view port bound in the view world:
        // .-------------------------.
        // |          .---.          |
        // | View     | V |          |
        // | Canvas   | P |          |
        // |          '---'          |
        // |                         |
        // '-------------------------'
        mTransformHelper.getValues(mTmpMatrix)
        val tx = mTransformHelper.translationX
        val ty = mTransformHelper.translationY
        val s = mTransformHelper.scaleX
        val vx = -tx / s / mScaleM2V
        val vy = -ty / s / mScaleM2V
        val vw = mViewPortMax.width / s
        val vh = mViewPortMax.height / s

        return Rect(vx, vy, vx + vw, vy + vh)
    }

    private fun constraintViewPort(viewPort: Rect,
                                   left: Float,
                                   top: Float,
                                   right: Float,
                                   bottom: Float,
                                   minWidth: Float,
                                   minHeight: Float,
                                   maxWidth: Float,
                                   maxHeight: Float): Rect {
        val bound = Rect(left = viewPort.left,
                         top = viewPort.top,
                         right = viewPort.right,
                         bottom = viewPort.bottom)

        // In width...
        if (bound.width < minWidth) {
            val cx = bound.centerX
            bound.left = cx - minWidth / 2f
            bound.right = cx + minWidth / 2f
        } else if (bound.width > maxWidth) {
            val cx = bound.centerX
            bound.left = cx - maxWidth / 2f
            bound.right = cx + maxWidth / 2f
        }
        // In height...
        if (bound.height < minHeight) {
            val cy = bound.centerY
            bound.top = cy - minHeight / 2f
            bound.bottom = cy + minHeight / 2f
        } else if (bound.height > maxHeight) {
            val cy = bound.centerY
            bound.top = cy - maxHeight / 2f
            bound.bottom = cy + maxHeight / 2f
        }
        // In x...
        val viewPortWidth = bound.width
        if (bound.left < left) {
            bound.left = left
            bound.right = bound.left + viewPortWidth
        } else if (bound.right > right) {
            bound.right = right
            bound.left = bound.right - viewPortWidth
        }
        // In y...
        val viewPortHeight = bound.height
        if (bound.top < top) {
            bound.top = top
            bound.bottom = bound.top + viewPortHeight
        } else if (bound.bottom > bottom) {
            bound.bottom = bottom
            bound.top = bound.bottom - viewPortHeight
        }

        return bound
    }

    /**
     * Convert the point from View (view port) world to Model world.
     */
    private fun toModelWorld(x: Float,
                             y: Float): FloatArray {
        // View might have padding, if so we need to subtract the padding to get
        // the position in the real view port.
        mTmpPoint[0] = x - ViewCompat.getPaddingStart(this)
        mTmpPoint[1] = y - this.paddingTop

        // Map the point from screen (view port) to the view canvas world.
        mCanvasMatrixInverse.mapPoints(mTmpPoint)

        // The point is still in the View world, we still need to map it to the
        // Model world.
        val scaleM2V = mScaleM2V
        mTmpPoint[0] = mTmpPoint[0] / scaleM2V
        mTmpPoint[1] = mTmpPoint[1] / scaleM2V

        return mTmpPoint
    }

    /**
     * Convert the point from Model world to View canvas world.
     */
    private fun toViewWorld(x: Float,
                            y: Float): FloatArray {
        val scaleM2V = mScaleM2V

        // Map the point from Model world to View world.
        mTmpPoint[0] = scaleM2V * x
        mTmpPoint[1] = scaleM2V * y

        return mTmpPoint
    }

    // Common Gesture /////////////////////////////////////////////////////////

    // Gesture.
    private val mTouchSlop by lazy { resources.getDimension(R.dimen.touch_slop) }
    private val mTapSlop by lazy { resources.getDimension(R.dimen.tap_slop) }
    private val mMinFlingVec by lazy { resources.getDimension(R.dimen.fling_min_vec) }
    private val mMaxFlingVec by lazy { resources.getDimension(R.dimen.fling_max_vec) }
    private val mGestureDetector by lazy {
        GestureDetector(Looper.getMainLooper(),
                        ViewConfiguration.get(context),
                        mTouchSlop,
                        mTapSlop,
                        mMinFlingVec,
                        mMaxFlingVec)
    }
    private var mIfHandleTouch = false
    private var mIfHandleDrag = false
    private val mGestureHistory = mutableListOf<GestureRecord>()

    private val mLastFocusPointer = Point()
    private var mMinPathSegment = 8f * getOneDp()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, null, null)
    }

    /**
     * Convert the [GestureEvent] to [CanvasEvent].
     */
    private fun touchEventToCanvasEvent(): ObservableTransformer<GestureEvent, CanvasEvent> {
        return ObservableTransformer { upstream ->
            upstream
                .map { event ->
                    when (event) {
                        is TouchBeginEvent,
                        is TouchEndEvent -> handleTouchLifecycleEvent(event)

                        is TapEvent -> handleTapEvent(event)

                        is DragBeginEvent,
                        is OnDragEvent,
                        is DragEndEvent -> handleDragEvent(event)

                        is PinchBeginEvent,
                        is OnPinchEvent,
                        is PinchEndEvent -> handlePinchEvent(event)

                        else -> NullCanvasEvent
                    }
                }
        }
    }

    private val mOtherCanvasEventSources = mutableListOf<Observable<CanvasEvent>>()

    /**
     * For [CanvasEvent] external sources.
     */
    fun addCanvasEventSource(source: Observable<CanvasEvent>) {
        mOtherCanvasEventSources.add(source)
    }

    private fun handleTouchLifecycleEvent(event: GestureEvent): CanvasEvent {
        when (event) {
            is TouchBeginEvent -> {
                mGestureHistory.clear()
                mWidget.handleTouchBegin()
            }
            is TouchEndEvent -> {
                mWidget.handleTouchEnd()
            }
        }

        // Null action
        return NullCanvasEvent
    }

    private fun handleTapEvent(event: TapEvent): CanvasEvent {
        mGestureHistory.add(GestureRecord.TAP)

        val (nx, ny) = toModelWorld(event.downX,
                                    event.downY)
        mWidget.drawDot(nx, ny)

        // Null action
        return NullCanvasEvent
    }

    private fun handleDragEvent(event: GestureEvent): CanvasEvent {
        if (event is DragBeginEvent) {
            mGestureHistory.add(GestureRecord.DRAG)

            // If there is NO PINCH in the history, do drag;
            // Otherwise, do view port transform.
            mIfHandleDrag = mGestureHistory.indexOf(GestureRecord.PINCH) == -1
        }

        return if (mIfHandleDrag) {
            when (event) {
                is DragBeginEvent -> {
                    val x = event.startPointer.x
                    val y = event.startPointer.y

                    val (nx, ny) = toModelWorld(x, y)
                    mWidget.beingDrawCurve(nx, ny)

                    // Hold focus x and y
                    mLastFocusPointer.set(x, y)
                }
                is OnDragEvent -> {
                    val x = event.stopPointer.x
                    val y = event.stopPointer.y

                    val dx = x - mLastFocusPointer.x
                    val dy = y - mLastFocusPointer.y
                    if (dx * dx + dy * dy >= mMinPathSegment * mMinPathSegment) {
                        val (nx, ny) = toModelWorld(x, y)
                        mWidget.drawCurveTo(nx, ny)

                        // Hold focus x and y
                        mLastFocusPointer.set(x, y)
                    }
                }
                is DragEndEvent -> {
                    mWidget.stopDrawCurve()
                }
            }

            // Null action
            NullCanvasEvent
        } else {
            when (event) {
                is DragBeginEvent -> {
                    ViewPortBeginUpdateEvent()
                }
                is OnDragEvent -> {
                    ViewPortOnUpdateEvent(
                        calculateViewPortBound(
                            startPointers = Array(2) { _ -> event.startPointer },
                            stopPointers = Array(2) { _ -> event.stopPointer }))
                }
                is DragEndEvent -> {
                    ViewPortStopUpdateEvent()
                }
                else -> {
                    // Null action
                    NullCanvasEvent
                }
            }
        }
    }

    private fun handlePinchEvent(event: GestureEvent): CanvasEvent {
        return when (event) {
            is PinchBeginEvent -> {
                mGestureHistory.add(GestureRecord.PINCH)
                ViewPortBeginUpdateEvent()
            }
            is OnPinchEvent -> {
                ViewPortOnUpdateEvent(
                    calculateViewPortBound(
                        startPointers = event.startPointers,
                        stopPointers = event.stopPointers))
            }
            is PinchEndEvent -> {
                ViewPortStopUpdateEvent()
            }
            else -> {
                // Null action
                NullCanvasEvent
            }
        }
    }

    // Context ////////////////////////////////////////////////////////////////

    // FIXME: Move to drawing section
    private var mIfShowPathJoints = false
    override val ifShowPathJoints: Boolean
        get() = mIfShowPathJoints

    // FIXME: Move to drawing section
    private var mPathInterpolatorID: String = resources.getString(R.string.prefs_path_interpolator_cubic_bezier)
    private fun createSvgDrawable(event: CanvasEvent): SVGDrawable {
        return when (event) {
            is StartSketchEvent -> {
                when (mPathInterpolatorID) {
                    resources.getString(R.string.prefs_path_interpolator_cubic_hermite) -> {
                        SVGHermiteCubicDrawable(id = event.svg,
                                                context = this@PaperCanvasView,
                                                penColor = event.penColor,
                                                penSize = getScaledPenSize(event),
                                                porterDuffMode = getPaintMode(event.penType))
                    }
                    resources.getString(R.string.prefs_path_interpolator_cubic_bezier) -> {
                        SVGCubicBezierDrawable(id = event.svg,
                                               context = this@PaperCanvasView,
                                               penColor = event.penColor,
                                               penSize = getScaledPenSize(event),
                                               porterDuffMode = getPaintMode(event.penType))
                    }
                    resources.getString(R.string.prefs_path_interpolator_linear) -> {
                        SVGLinearDrawable(id = event.svg,
                                          context = this@PaperCanvasView,
                                          penColor = event.penColor,
                                          penSize = getScaledPenSize(event),
                                          porterDuffMode = getPaintMode(event.penType))
                    }
                    else -> throw IllegalArgumentException("Invalid path interpolator ID")
                }
            }
            // TODO: Remove this ugly thing, and replace it with drawing event!
            is AddSvgEvent -> {
                when (mPathInterpolatorID) {
                    resources.getString(R.string.prefs_path_interpolator_cubic_hermite) -> {
                        SVGHermiteCubicDrawable(id = event.strokeID,
                                                context = this@PaperCanvasView,
                                                points = event.points.map { p ->
                                                    val (x, y) = toViewWorld(p.x, p.y)
                                                    Point(x, y)
                                                },
                                                penColor = event.penColor,
                                                penSize = getScaledPenSize(event),
                                                porterDuffMode = getPaintMode(event.penType))
                    }
                    resources.getString(R.string.prefs_path_interpolator_cubic_bezier) -> {
                        SVGCubicBezierDrawable(id = event.strokeID,
                                               context = this@PaperCanvasView,
                                               points = event.points.map { p ->
                                                   val (x, y) = toViewWorld(p.x, p.y)
                                                   Point(x, y)
                                               },
                                               penColor = event.penColor,
                                               penSize = getScaledPenSize(event),
                                               porterDuffMode = getPaintMode(event.penType))
                    }
                    else -> {
                        SVGLinearDrawable(id = event.strokeID,
                                          context = this@PaperCanvasView,
                                          points = event.points.map { p ->
                                              val (x, y) = toViewWorld(p.x, p.y)
                                              Point(x, y)
                                          },
                                          penColor = event.penColor,
                                          penSize = getScaledPenSize(event),
                                          porterDuffMode = getPaintMode(event.penType))
                    }
                }
            }
            else -> throw IllegalArgumentException("Invalid event")
        }
    }

    override fun getOneDp(): Float {
        return mOneDp
    }

    override fun getViewConfiguration(): ViewConfiguration {
        return ViewConfiguration.get(context)
    }

    override fun getMinPenSize(): Float {
        return mScaleM2V * ModelConst.MIN_PEN_SIZE
    }

    override fun getMaxPenSize(): Float {
        return mScaleM2V * ModelConst.MAX_PEN_SIZE
    }

    override fun getTouchSlop(): Float {
        return mTouchSlop
    }

    override fun getTapSlop(): Float {
        return mTapSlop
    }

    override fun getMinFlingVec(): Float {
        return mMinFlingVec
    }

    override fun getMaxFlingVec(): Float {
        return mMaxFlingVec
    }

    override fun mapM2V(x: Float, y: Float): FloatArray {
        return toViewWorld(x, y)
    }

    override fun mapM2V(v: Float): Float {
        return mScaleM2V * v
    }

    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException("Not in MAIN thread")
        }
    }

    private fun ensureNotMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalThreadStateException("Should NOT in MAIN thread")
        }
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already start to a widget")
    }

    private fun drawBackground(canvas: Canvas,
                               vw: Float,
                               vh: Float,
                               tx: Float,
                               ty: Float,
                               scaleVP: Float) {
        if (scaleVP <= 1f) return

        val scaledVW = scaleVP * vw
        val scaledVH = scaleVP * vh
        val cell = Math.max(scaledVW, scaledVH) / 16
        val crossHalfW = 7f * mOneDp

        // The closer to base view port, the smaller the alpha is;
        // So the convex set is:
        //
        // Base                 Min
        // |-------x-------------|
        // x = (1 - a) * Base + a * Min = scaleVP
        // thus...
        //       vpW - baseW
        // a = --------------
        //      minW - baseW
        val alpha = (mViewPort.width - mViewPortBase.width) /
                    (mViewPortMin.width - mViewPortBase.width)
        mGridPaint.alpha = (alpha * 0xFF).toInt()

        // Cross
        val left = tx - cell
        val right = tx + scaledVW + cell
        val top = ty - cell
        val bottom = ty + scaledVH + cell
        var y = top
        while (y <= bottom) {
            var x = left
            while (x <= right) {
                canvas.drawLine(x - crossHalfW, y, x + crossHalfW, y, mGridPaint)
                canvas.drawLine(x, y - crossHalfW, x, y + crossHalfW, mGridPaint)
                x += cell
            }
            y += cell
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaperCanvasView

        if (mScrapViews != other.mScrapViews) return false
//        if (mStrokeDrawables != other.mStrokeDrawables) return false
        if (mViewPortMin != other.mViewPortMin) return false
        if (mViewPortMax != other.mViewPortMax) return false
        if (mViewPortBase != other.mViewPortBase) return false

        return true
    }

    private var mIsHashDirty = true
    private var mHashCode = AppConst.EMPTY_HASH

    override fun hashCode(): Int {
//        return if (mScrapViews.isEmpty() && mStrokeDrawables.isEmpty()) {
        return if (mScrapViews.isEmpty()) {
            AppConst.EMPTY_HASH
        } else {
            // FIXME: Consider scraps hash too.

            if (mIsHashDirty) {
                mHashCode = AppConst.EMPTY_HASH
//                mStrokeDrawables.forEach { d ->
//                    cacheHashCode = 31 * cacheHashCode + d.hashCode()
//                }
                mScrapViews.forEach { v ->
                    mHashCode = 31 * mHashCode + v.hashCode()
                }

                mIsHashDirty = false
            }

            mHashCode
        }
    }
}