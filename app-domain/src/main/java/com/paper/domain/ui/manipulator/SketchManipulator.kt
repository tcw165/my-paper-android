// Copyright Aug 2018-present Paper
//
// Author: boyw165@gmail.com
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

package com.paper.domain.ui.manipulator

import com.cardinalblue.gesture.rx.*
import com.paper.model.ISchedulers
import com.paper.domain.ui.BaseManipulator
import com.paper.model.repository.EditorOperation
import com.paper.domain.ui.SVGScrapWidget
import com.paper.domain.ui.SimpleEditorWidget
import com.paper.domain.ui_event.AddScrapEvent
import com.paper.model.Frame
import com.paper.model.IPaper
import com.paper.model.Point
import com.paper.model.SVGScrap
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.addTo
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class SketchManipulator(private val editor: SimpleEditorWidget,
                        private val paper: Single<IPaper>,
                        private val highestZ: Int,
                        private val schedulers: ISchedulers)
    : BaseManipulator() {

    override fun apply(upstream: Observable<GestureEvent>): ObservableSource<EditorOperation> {
        if (upstream !is DragGestureObservable) return Observable.empty()

        return autoStop { emitter ->
            disposableBag.clear()

            val id = UUID.randomUUID()
            // Observe widget creation
            val widgetSignal = editor.observeScraps()
                .filter { event ->
                    event is AddScrapEvent &&
                    event.scrapWidget.getID() == id
                }
                .firstElement()
                .map { event ->
                    event as AddScrapEvent
                    event.scrapWidget as SVGScrapWidget
                }
                .cache()
            val startX = AtomicReference(0f)
            val startY = AtomicReference(0f)

            // Begin, add scrap and create corresponding widget
            Maybes.zip(paper.toMaybe(),
                       upstream.firstElement())
                .observeOn(schedulers.main())
                .subscribe { (paper, event) ->
                    event as DragBeginEvent

                    val (x, y) = event.startPointer
                    val scrap = createSVGScrap(id, x, y)

                    // Remember start x-y for later point correction
                    startX.set(x)
                    startY.set(y)

                    paper.addScrap(scrap)
                }
                .addTo(disposableBag)

            val pointSrc = upstream.map { event ->
                when (event) {
                    is DragBeginEvent -> {
                        val (x, y) = event.startPointer
                        val nx = x - startX.get()
                        val ny = y - startY.get()

                        Point(nx, ny)
                    }
                    is DragDoingEvent -> {
                        val (x, y) = event.stopPointer
                        val nx = x - startX.get()
                        val ny = y - startY.get()

                        Point(nx, ny)
                    }
                    is DragEndEvent -> {
                        val (x, y) = event.stopPointer
                        val nx = x - startX.get()
                        val ny = y - startY.get()

                        Point(nx, ny)
                    }
                    else -> TODO()
                }
            }

            // Delegate to widget
            widgetSignal
                .observeOn(schedulers.main())
                .flatMap { widget ->
                    widget.handleSketch(pointSrc)
                }
                .subscribe { operation ->
                    // Pass result operation
                    emitter.onNext(operation)
                }
                .addTo(disposableBag)
        }
    }

    private fun createSVGScrap(id: UUID,
                               x: Float,
                               y: Float): SVGScrap {
        return SVGScrap(uuid = id,
                        frame = Frame(x = x,
                                      y = y,
                                      scaleX = 1f,
                                      scaleY = 1f,
                                      width = 1f,
                                      height = 1f,
                                      z = highestZ + 1))
    }
}
