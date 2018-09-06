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

package com.paper.domain

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardEditorWidgetTest : BaseDomainTest() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    override fun clean() {
        super.clean()
    }

    @Test
    fun `sketching, editor should be busy`() {
//        val tester = WhiteboardWidget(schedulers = mockSchedulers)
//
//        val busyTest = tester
//            .onBusy()
//            .test()
//
//        // Start widget
//        tester.inject(mockPaper)
//        tester.start().test().assertSubscribed()
//
//        // Make sure the stream moves
//        moveScheduler()
//
//        // Sketch (by simulating the gesture interpreter behavior)
//        val widget = SVGScrapWidget(
//            scrap = SVGScrap(frame = createRandomFrame()),
//            schedulers = mockSchedulers)
//        tester.handleDomainEvent(GroupEditorEvent(
//            listOf(AddScrapEvent(widget),
//                   FocusScrapEvent(widget.getID()),
//                   StartSketchEvent(0f, 0f))))
//
//        // Trigger for see sketch event
//        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)
//
//        busyTest.assertValueAt(busyTest.valueCount() - 1, true)
    }
}
