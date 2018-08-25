// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.domain.ui

import com.paper.domain.ISchedulerProvider
import com.paper.model.Frame
import com.paper.model.IScrap
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

open class BaseScrapWidget(private val scrap: IScrap,
                           protected val schedulers: ISchedulerProvider)
    : IBaseScrapWidget {

    protected val mLock = Any()

    protected val mCancelSignal = PublishSubject.create<Any>().toSerialized()
    protected val mDisposables = CompositeDisposable()

    override fun start() {
        synchronized(mLock) {
            ensureNoLeakedBinding()
        }
    }

    override fun stop() {
        synchronized(mLock) {
            mCancelSignal.onNext(0)
            mDisposables.clear()
        }
    }

    override fun getID(): UUID {
        return scrap.getID()
    }

    // Frame //////////////////////////////////////////////////////////////////

    private val mFrameSignal = PublishSubject.create<Frame>().toSerialized()

    override fun getFrame(): Frame {
        synchronized(mLock) {
            return scrap.getFrame()
        }
    }

    fun setFrame(frame: Frame) {
        synchronized(mLock) {
            scrap.setFrame(frame)

            // Signal out
            mFrameSignal.onNext(frame)
        }
    }

    override fun onUpdateFrame(): Observable<Frame> {
        return mFrameSignal
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already start a model")
    }
}