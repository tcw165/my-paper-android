// Copyright Apr 2017-present Paper
//
// Author: boyw165@gmail.com,
//         djken0106@gmail.com
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

package com.paper.model

import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import java.io.File
import java.util.*
import java.util.concurrent.locks.Lock

interface IPaper : Lock {

    // The SQLite ID.
    fun getId(): Long
    // The global ID.
    fun getUUID(): UUID

    fun getCreatedAt(): Long

    fun getModifiedAt(): Long
    fun setModifiedAt(time: Long)

    // By default is landscape A4, 210 x 297 units.
    fun getWidth(): Float
    fun getHeight(): Float

    fun setWidth(width: Float)
    fun setHeight(height: Float)

    fun getThumbnail(): File?
    fun getThumbnailWidth(): Int
    fun getThumbnailHeight(): Int

    fun setThumbnail(file: File)
    fun setThumbnailWidth(width: Int)
    fun setThumbnailHeight(height: Int)

    fun getCaption(): String
    fun getTags(): List<String>

    // Sketch & strokes ///////////////////////////////////////////////////////

    fun getSketch(): List<SketchStroke>

    fun pushStroke(stroke: SketchStroke)

    fun popStroke(): SketchStroke

    fun removeAllStrokes()

    fun onAddStroke(replayAll: Boolean = true): Observable<SketchStroke>

    fun onRemoveStroke(): Observable<SketchStroke>

    // Scraps /////////////////////////////////////////////////////////////////

    fun getScraps(): List<Scrap>

    fun addScrap(scrap: Scrap)

    fun removeScrap(scrap: Scrap)

    fun onAddScrap(replayAll: Boolean = true): Observable<Scrap>

    fun onRemoveScrap(): Observable<Scrap>
}
