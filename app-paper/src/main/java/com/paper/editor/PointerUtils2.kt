//  Copyright Oct 2017-present CardinalBlue
//
//  Author: boy@cardinalblue.com
//          jack.huang@cardinalblue.com
//          yolung.lu@cardinalblue.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.paper.editor

import android.graphics.PointF

object PointerUtils2 {

    val DELTA_X = 0
    val DELTA_Y = 1
    val DELTA_SCALE_X = 2
    val DELTA_SCALE_Y = 3
    val DELTA_RADIANS = 4
    val PIVOT_X = 5
    val PIVOT_Y = 6

    /**
     * Get an array of [tx, ty, sx, sy, rotation] sequence representing
     * the transformation from the given event.
     */
    fun getTransformFromPointers(startPointers: Array<PointF>,
                                 stopPointers: Array<PointF>): FloatArray {
        if (startPointers.size < 2 || stopPointers.size < 2) {
            throw IllegalStateException(
                "The event must have at least two down pointers.")
        }

        val transform = floatArrayOf(
            // delta x
            0f,
            // delta y
            0f,
            // delta scale x
            1f,
            // delta scale y
            1f,
            // delta rotation in radians
            0f,
            // pivot x.
            0f,
            // pivot y.
            0f)

        // Start pointer 1.
        val startX1 = startPointers[0].x
        val startY1 = startPointers[0].y
        // Start pointer 2.
        val startX2 = startPointers[1].x
        val startY2 = startPointers[1].y

        // Stop pointer 1.
        val stopX1 = stopPointers[0].x
        val stopY1 = stopPointers[0].y
        // Stop pointer 2.
        val stopX2 = stopPointers[1].x
        val stopY2 = stopPointers[1].y

        // Start vector.
        val startVecX = startX2 - startX1
        val startVecY = startY2 - startY1
        // Stop vector.
        val stopVecX = stopX2 - stopX1
        val stopVecY = stopY2 - stopY1

        // Start pivot.
        val startPivotX = (startX1 + startX2) / 2f
        val startPivotY = (startY1 + startY2) / 2f
        // Stop pivot.
        val stopPivotX = (stopX1 + stopX2) / 2f
        val stopPivotY = (stopY1 + stopY2) / 2f

        // Calculate the translation.
        transform[DELTA_X] = stopPivotX - startPivotX
        transform[DELTA_Y] = stopPivotY - startPivotY
        // Calculate the rotation degree.
        transform[DELTA_RADIANS] = (Math.atan2(stopVecY.toDouble(), stopVecX.toDouble()) - Math.atan2(startVecY.toDouble(), startVecX.toDouble())).toFloat()
        // Calculate the scale change.
        val dScale = (Math.hypot(stopVecX.toDouble(),
                                 stopVecY.toDouble()) / Math.hypot(startVecX.toDouble(),
                                                                   startVecY.toDouble())).toFloat()
        transform[DELTA_SCALE_X] = dScale
        transform[DELTA_SCALE_Y] = dScale
        transform[PIVOT_X] = startPivotX
        transform[PIVOT_Y] = startPivotY

        return transform
    }
}
