// Copyright (c) 2017-present WANG, TAI-CHUN
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper.exp.simulation

import android.graphics.Canvas
import android.graphics.Paint
import io.reactivex.Observable

abstract class CollisionSystemContract private constructor() {

    interface View {

        fun schedulePeriodicRendering(listener: SimulationListener)
        fun unScheduleAll()

        fun getParticlePaint(): Paint
        fun getCanvasWidth(): Int
        fun getCanvasHeight(): Int

        fun showToast(text: String)
        fun showText(canvas: Canvas, text: String)

        fun onClickBack(): Observable<Any>
    }

    interface SimulationListener {

        fun onUpdateSimulation(canvas: Canvas)
    }
}