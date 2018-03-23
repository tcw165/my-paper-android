// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.gallery.view

import com.airbnb.epoxy.TypedEpoxyController
import com.bumptech.glide.RequestManager
import com.paper.shared.model.PaperModel

class PaperThumbnailEpoxyController(
    private val mGlide: RequestManager)
    : TypedEpoxyController<List<PaperModel>>() {

    private var mListener: IOnClickPaperThumbnailListener? = null

    override fun buildModels(data: List<PaperModel>) {
        data.forEachIndexed { _, paper ->
            val id = paper.id
            val uuid = paper.uuid

            PaperThumbnailEpoxyModel(id)
                .setThumbnail(mGlide,
                              paper.thumbnailPath,
                              paper.thumbnailWidth,
                              paper.thumbnailHeight)
                .setClickListener(mListener)
                // Epoxy view-model ID.
                .id(uuid.toString())
                .addTo(this)
        }
    }

    fun setOnClickPaperThumbnailListener(listener: IOnClickPaperThumbnailListener?) {
        mListener = listener
    }
}