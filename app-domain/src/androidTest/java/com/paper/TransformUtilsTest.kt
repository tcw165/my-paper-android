// Copyright Apr 2018-present Paper
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

package com.paper

import android.graphics.PointF
import android.support.test.runner.AndroidJUnit4

import com.paper.domain.util.TransformUtils

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransformUtilsTest {

    @Test
    @Throws(Exception::class)
    fun orientation_isCorrect() {
        // ^
        // |
        // | 3
        // |      2
        // |1
        // +--------->
        Assert.assertEquals(TransformUtils.ORIENTATION_CCW,
                            TransformUtils.getOrientation(
                                PointF(1f, 1f),
                                PointF(6f, 2f),
                                PointF(2f, 3f)))
        // ^
        // |
        // | 2
        // |      3
        // |1
        // +--------->
        Assert.assertEquals(TransformUtils.ORIENTATION_CW,
                            TransformUtils.getOrientation(
                                PointF(1f, 1f),
                                PointF(2f, 3f),
                                PointF(6f, 2f)))
        // ^
        // |
        // |      3
        // |   2
        // |1
        // +--------->
        Assert.assertEquals(TransformUtils.ORIENTATION_COLLINEAR,
                            TransformUtils.getOrientation(
                                PointF(1f, 1f),
                                PointF(2f, 2f),
                                PointF(3f, 3f)))
    }
}
