//  Copyright Oct 2017-present CardinalBlue
//
//  Author: boy@cardinalblue.com
//          jack.huang@cardinalblue.com
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

package com.my.widget.gesture;

import android.view.MotionEvent;

public interface IGestureListener {

    void onActionBegin();

    void onActionEnd();

    void onSingleTap(MyMotionEvent event,
                     Object touchingObject,
                     Object touchingContext);

    void onDoubleTap(MyMotionEvent event,
                     Object touchingObject,
                     Object touchingContext);

    void onMoreTap(MyMotionEvent event,
                   Object touchingObject,
                   Object touchingContext,
                   int tapCount);

    void onLongTap(MyMotionEvent event,
                   Object touchingObject,
                   Object touchingContext);

    void onLongPress(MyMotionEvent event,
                     Object touchingObject,
                     Object touchingContext);

    // Drag ///////////////////////////////////////////////////////////////

    boolean onDragBegin(MyMotionEvent event,
                        Object touchingObject,
                        Object touchingContext,
                        float xInCanvas,
                        float yInCanvas);

    void onDrag(MyMotionEvent event,
                Object touchingObject,
                Object touchingContext,
                float[] translationInCanvas);

    void onDragEnd(MyMotionEvent event,
                   Object touchingObject,
                   Object touchingContext,
                   float[] translationInCanvas);

    // Fling //////////////////////////////////////////////////////////////

    /**
     * Notified of a fling event when it occurs with the initial on down
     * {@link MotionEvent} and the matching up {@link MotionEvent}. The
     * calculated velocity is supplied along the x and y axis in pixels per
     * second.
     *
     * @param event                The MotionEvent alternative.
     * @param startPointerInCanvas The first down pointer that started the
     *                             fling.
     * @param stopPointerInCanvas  The move pointer that triggered the
     *                             current onFling.
     * @param velocityX            The velocity of this fling measured in
     *                             pixels per second along the x axis.
     * @param velocityY            The velocity of this fling measured in
     */
    boolean onFling(MyMotionEvent event,
                    Object touchingObject,
                    Object touchContext,
                    float[] startPointerInCanvas,
                    float[] stopPointerInCanvas,
                    float velocityX,
                    float velocityY);

    // Pinch //////////////////////////////////////////////////////////////

    boolean onPinchBegin(MyMotionEvent event,
                         Object touchingObject,
                         Object touchContext,
                         float pivotXInCanvas,
                         float pivotYInCanvas);

    void onPinch(MyMotionEvent event,
                 Object touchingObject,
                 Object touchContext,
                 float[] startPointerOneInCanvas,
                 float[] startPointerTwoInCanvas,
                 float[] stopPointerOneInCanvas,
                 float[] stopPointerTwoInCanvas);

    void onPinchEnd(MyMotionEvent event,
                    Object touchingObject,
                    Object touchContext,
                    float[] startPointerOneInCanvas,
                    float[] startPointerTwoInCanvas,
                    float[] stopPointerOneInCanvas,
                    float[] stopPointerTwoInCanvas);
}
