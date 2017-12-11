package com.paper.editor

import com.cardinalblue.gesture.GestureDetector
import com.paper.shared.model.TransformModel

class PaperCanvasContract private constructor() {

    interface BaseView {

        fun getTransform(): TransformModel

        fun setTransform(transform: TransformModel, pivotX: Float, pivotY: Float)

        fun convertPointFromChildToParent(point: FloatArray)

        fun setInterceptTouchEvent(enabled: Boolean)

        fun setGestureDetector(detector: GestureDetector?)
    }

    interface ParentView : BaseView {

//        fun addScrapView(data: Any)
//
//        fun removeScrapView(data: Any)
    }

    interface Config {

        fun getTouchSlop(): Float

        fun getTapSlop(): Float

        fun getMinFlingVec(): Float

        fun getMaxFlingVec(): Float
    }
}
