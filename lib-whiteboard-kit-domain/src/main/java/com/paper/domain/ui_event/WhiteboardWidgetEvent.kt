// Copyright Aug 2018-present Paper
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

package com.paper.domain.ui_event

import com.paper.domain.ui.ScrapWidget
import com.paper.model.Rect
import java.util.*

sealed class WhiteboardWidgetEvent

data class GroupEditorEvent(val events: List<WhiteboardWidgetEvent>) : WhiteboardWidgetEvent()

// Lifecycle //////////////////////////////////////////////////////////////////

abstract class EditorTouchLifecycleEvent : WhiteboardWidgetEvent()

object TouchBeginEvent : EditorTouchLifecycleEvent()

object TouchEndEvent : EditorTouchLifecycleEvent()

// Add/remove/focus scrap /////////////////////////////////////////////////////

abstract class UpdateScrapEvent : WhiteboardWidgetEvent()

data class AddScrapEvent(val scrapWidget: ScrapWidget) : UpdateScrapEvent()

data class RemoveScrapEvent(val scrapWidget: ScrapWidget) : UpdateScrapEvent()

data class FocusScrapEvent(val scrapID: UUID) : UpdateScrapEvent()

object ClearFocusEvent : UpdateScrapEvent()

data class HighlightScrapEvent(val scrapID: UUID) : UpdateScrapEvent()

object ClearHighlightEvent : UpdateScrapEvent()

// View-port //////////////////////////////////////////////////////////////////

/**
 * The action representing the operation to view-port.
 * @see [ViewPortBeginUpdateEvent]
 * @see [ViewPortOnUpdateEvent]
 * @see [ViewPortStopUpdateEvent]
 */
abstract class ViewPortEvent : WhiteboardWidgetEvent()

/**
 * A start signal indicating the view-port is about to update.
 */
class ViewPortBeginUpdateEvent : ViewPortEvent()

/**
 * A doing signal indicating the view-port is about to update.
 *
 * @param bound The desired boundary for the view-port.
 */
data class ViewPortOnUpdateEvent(val bound: Rect) : ViewPortEvent()

/**
 * A stop signal indicating the end of the view-port operation.
 */
class ViewPortStopUpdateEvent : ViewPortEvent()