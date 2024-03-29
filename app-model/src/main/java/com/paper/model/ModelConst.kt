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

package com.paper.model

object ModelConst {

    const val TAG = "paper model"

    const val TEMP_ID = -1L
    const val INVALID_ID = Long.MAX_VALUE

    const val MOST_TOP_Z = Long.MAX_VALUE
    const val MOST_BOTTOM_Z = 0L
    const val INVALID_Z = -2L

    val SIZE_OF_A_FOUR_LANDSCAPE = Pair(297f, 210f)
    val SIZE_OF_A_FOUR_PORTRAIT = Pair(210f, 297f)
    val SIZE_OF_A_FOUR_SQUARE = Pair(210f, 210f)

    const val MIN_PEN_SIZE = 1f
    const val MAX_PEN_SIZE = 60f

    const val PREFS_BROWSE_PAPER_ID = "prefs_browse_paper_id"
}
