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
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.paper.R
import io.reactivex.Observer

class CreatePaperEpoxyViewModel : EpoxyModelWithHolder<EpoxyHolder>() {

    override fun getDefaultLayout(): Int {
        return R.layout.gallery_item_of_create_paper
    }

    override fun createNewHolder(): EpoxyHolder {
        return CreatePaperEpoxyViewModel.Holder()
    }

    override fun bind(holder: EpoxyHolder) {
        super.bind(holder)

        // Smart casting
        holder as CreatePaperEpoxyViewModel.Holder

        holder.itemView.setOnClickListener {
            mOnClickSignal?.onNext(0)
        }
    }

    override fun unbind(holder: EpoxyHolder?) {
        super.unbind(holder)

        // Smart casting
        holder as CreatePaperEpoxyViewModel.Holder

        holder.itemView.setOnClickListener(null)
    }

    // Click //////////////////////////////////////////////////////////////////

    private var mOnClickSignal: Observer<Any>? = null

    fun onClick(clickSignal: Observer<Any>): CreatePaperEpoxyViewModel {
        mOnClickSignal = clickSignal
        return this
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        return other is CreatePaperEpoxyViewModel
    }

    override fun hashCode(): Int {
        return 0
    }

    // Clazz //////////////////////////////////////////////////////////////////

    class Holder : EpoxyHolder() {

        lateinit var itemView: View

        override fun bindView(itemView: View) {
            this.itemView = itemView
        }
    }
}
