// Copyright Apr 2018-present boyw165@gmail.com
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

package com.paper.view.editPanel

import com.airbnb.epoxy.TypedEpoxyController
import com.bumptech.glide.RequestManager
import com.paper.R
import com.paper.domain.ui.EditorMode
import com.paper.domain.ui_event.EditorModeEvent
import com.paper.domain.ui.PaperMenuWidget

class ToolListEpoxyController(imageLoader: RequestManager)
    : TypedEpoxyController<EditorModeEvent>() {

    private val mImgLoader = imageLoader

    override fun buildModels(data: EditorModeEvent) {
        data.mode.forEachIndexed { i, toolType ->
            ToolEpoxyViewModel(
                editorMode = toolType,
                imgLoader = mImgLoader,
                resourceId = getResourceId(toolType),
                isUsing = data.usingIndex == i,
                widget = mWidget)
                .id(toolType.ordinal)
                .addTo(this)
        }
    }

    private fun getResourceId(toolId: EditorMode): Int {
        return when (toolId) {
            EditorMode.SELECT_TO_DELETE -> R.drawable.sel_img_e_eraser
            EditorMode.FREE_DRAWING -> R.drawable.sel_img_e_pen
            EditorMode.SELECT_TO_DRAG_AND_DROP -> R.drawable.sel_img_e_scissor
            else -> throw IllegalArgumentException("Unsupported tool ID")
        }
    }

    private var mWidget: PaperMenuWidget? = null

    // TODO: Since the tool list (data) and UI click both lead to the
    // TODO: UI view-model change, merge this two upstream in the new
    // TODO: design!
    fun setWidget(widget: PaperMenuWidget?) {
        mWidget = widget
    }
}