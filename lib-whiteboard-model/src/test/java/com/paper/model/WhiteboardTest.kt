// Copyright May 2018-present Paper
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

package com.paper.model

import io.useful.itemAdded
import io.useful.itemRemoved
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.net.URI
import java.util.*

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardTest : BaseModelTest() {

    @Test
    fun `basic copy`() {
        val tester1 = Whiteboard(id = 1,
                                 uuid = UUID.randomUUID(),
                                 createdAt = 100L,
                                 modifiedAt = 200L,
                                 width = 500f,
                                 height = 500f,
                                 viewPort = Rect(0f, 0f, 500f, 500f))
        val tester2 = tester1.copy()
        tester2.size = Pair(300f, 300f)
        tester2.viewPort = Rect(125f, 125f, 250f, 250f)
        tester2.modifiedAt = 300L
        tester2.thumbnail = Triple(URI("file:///foo"), 100, 100)

        Assert.assertNotEquals(tester2, tester1)
        Assert.assertNotEquals(tester2.size, tester1.size)
        Assert.assertNotEquals(tester2.viewPort, tester1.viewPort)
        Assert.assertNotEquals(tester2.modifiedAt, tester1.modifiedAt)
    }

    @Test
    fun `scraps copy`() {
        val tester1 = Whiteboard()
        val tester2 = tester1.copy()
        tester2.addScrap(Scrap())

        Assert.assertNotEquals(tester2.scraps, tester1.scraps)
    }

    @Test
    fun `observe add scrap`() {
        val tester = Whiteboard()

        val addTestObserver = tester::scraps.itemAdded().test()

        tester.addScrap(createRandomScrap())
        tester.addScrap(createRandomScrap())
        tester.addScrap(createRandomScrap())

        addTestObserver.assertValueCount(3)
    }

    @Test
    fun `observe remove scrap`() {
        val scrap1 = createRandomScrap()
        val scrap2 = createRandomScrap()
        val scrap3 = createRandomScrap()
        val tester = Whiteboard(scraps = mutableSetOf(scrap1, scrap2, scrap3))

        val removeTestObserver = tester::scraps.itemRemoved().test()

        tester.removeScrap(scrap1)
        tester.removeScrap(scrap2)
        tester.removeScrap(scrap3)

        removeTestObserver.assertValueCount(3)
    }
}

