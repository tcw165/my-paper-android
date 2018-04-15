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

package com.paper.view.gallery

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.airbnb.epoxy.EpoxyModel
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.paper.R
import java.io.File

class PaperThumbnailEpoxyModel(
    private var mPaperId: Long)
    : EpoxyModel<View>() {

    private var mListener: IOnClickPaperThumbnailListener? = null

    private var mGlide: RequestManager? = null
    private var mThumbFile: File? = null
    private var mThumbWidth = 0
    private var mThumbHeight = 0

    // View
    private var mThumbView: ImageView? = null
    private var mThumbViewContainer: ViewGroup? = null

    override fun getDefaultLayout(): Int {
        return R.layout.item_paper_thumbnail
    }

    override fun bind(view: View) {
        super.bind(view)

        if (mThumbView == null) {
            mThumbView = view.findViewById(R.id.image_view)
        }
        if (mThumbViewContainer == null) {
            mThumbViewContainer = view.findViewById(R.id.image_view_container)
        }

        // Update thumbnail size by fixing width and changing the height.
        val layoutParams = view.layoutParams
        val layoutWidth = layoutParams.width
        val thumbRatio = mThumbWidth / mThumbHeight
        layoutParams.height = layoutWidth / thumbRatio
        view.layoutParams = layoutParams

        // Click
        view.setOnClickListener {
            mListener?.onClickPaperThumbnail(mPaperId)
        }

        // Thumb
        mThumbFile?.let { file ->
            mGlide?.load(file)
                ?.apply(RequestOptions
                            .skipMemoryCacheOf(true)
                            .priority(Priority.NORMAL)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .dontTransform())
                ?.into(mThumbView)
        }
    }

    override fun unbind(view: View?) {
        super.unbind(view)

        // Click
        view?.setOnClickListener(null)
    }

    fun setThumbnail(glide: RequestManager?,
                     file: File?,
                     width: Int,
                     height: Int): PaperThumbnailEpoxyModel {
        mGlide = glide
        mThumbFile = file
        mThumbWidth = width
        mThumbHeight = height
        return this
    }

    fun setClickListener(listener: IOnClickPaperThumbnailListener?): PaperThumbnailEpoxyModel {
        mListener = listener
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PaperThumbnailEpoxyModel

        if (mPaperId != other.mPaperId) return false
        if (mThumbFile != other.mThumbFile) return false
        if (mThumbWidth != other.mThumbWidth) return false
        if (mThumbHeight != other.mThumbHeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mPaperId.hashCode()
        result = 31 * result + (mThumbFile?.hashCode() ?: 0)
        result = 31 * result + mThumbWidth
        result = 31 * result + mThumbHeight
        return result
    }
}