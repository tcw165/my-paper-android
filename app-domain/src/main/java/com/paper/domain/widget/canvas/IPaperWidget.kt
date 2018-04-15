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

package com.paper.domain.widget.canvas

import com.paper.domain.event.DrawSVGEvent
import com.paper.model.PaperModel
import com.paper.model.Rect
import io.reactivex.Observable
import java.io.File

interface IPaperWidget : IBaseWidget<PaperModel> {

    // For input //////////////////////////////////////////////////////////////
    // TODO: How to define the inbox?

    fun handleSetThumbnail(file: File,
                           width: Int,
                           height: Int)

    fun handleChoosePenColor(color: Int)

    fun handleUpdatePenSize(size: Float)

    fun handleActionBegin()

    fun handleActionEnd()

    fun handleTap(x: Float, y: Float)

    fun handleDragBegin(x: Float, y: Float)

    fun handleDrag(x: Float, y: Float)

    fun handleDragEnd(x: Float, y: Float)

    // For output /////////////////////////////////////////////////////////////

    fun getPaper(): PaperModel

    fun onSetCanvasSize(): Observable<Rect>

    fun onAddScrapWidget(): Observable<IScrapWidget>

    fun onRemoveScrapWidget(): Observable<IScrapWidget>

    fun onDrawSVG(): Observable<DrawSVGEvent>

    fun onPrintDebugMessage(): Observable<String>
}