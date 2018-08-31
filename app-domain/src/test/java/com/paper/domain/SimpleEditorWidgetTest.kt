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

import com.paper.domain.ui.SimpleEditorWidget
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner.Silent::class)
class SimpleEditorWidgetTest : MockDataLayerTest() {

    @Test
    fun `busy test`() {
        val tester = SimpleEditorWidget(paperID = 0,
                                        paperRepo = mockPaperRepo,
                                        caughtErrorSignal = caughtErrorSignal,
                                        schedulers = mockSchedulers)

        val busyTest = tester
            .onBusy()
            .test()

        // Start widget
        val lifecycleTest = tester.start().test()
        lifecycleTest.assertSubscribed()

        // Advance time for initialization
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        busyTest.assertValueAt(0, true)
        busyTest.assertValueAt(busyTest.valueCount() - 1, false)
    }

    @Test
    fun `inflation process test`() {
        val tester = SimpleEditorWidget(paperID = 0,
                                        paperRepo = mockPaperRepo,
                                        caughtErrorSignal = caughtErrorSignal,
                                        schedulers = mockSchedulers)

        val scrapTester = tester.onUpdateScrap().test()

        // Start widget
        val lifecycleTest = tester.start().test()
        lifecycleTest.assertSubscribed()

        // Advance time for initialization
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        scrapTester.assertValueCount(mockPaper.getScraps().size)
    }
}
