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

package com.paper.domain.useCase

import com.paper.domain.DomainConst
import com.paper.domain.widget.editor.IWidget
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Bind the [IWidget] with the model and automatically destroy the binding if it
 * gets disposed.
 */
class BindWidgetWithModel<T>(widget: IWidget<T>,
                             model: T,
                             caughtErrorSignal: Observer<Throwable>? = null)
    : Observable<Boolean>() {

    private val mWidget = widget
    private val mModel = model

    private val mCaughtErrorSignal = caughtErrorSignal

    override fun subscribeActual(observer: Observer<in Boolean>) {
        val d = UnbindDisposable(mWidget, mCaughtErrorSignal)
        observer.onSubscribe(d)

        if (!d.isDisposed) {
            try {
                mWidget.bindModel(mModel)
                println("${DomainConst.TAG}: Bind widget [$mWidget] with the model")

                observer.onNext(true)
            } catch (err: Throwable) {
                observer.onNext(false)

                mCaughtErrorSignal?.onNext(err)
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class UnbindDisposable<T>(widget: IWidget<T>,
                                       caughtErrorSignal: Observer<Throwable>?) : Disposable {

        @Volatile
        private var disposed = false

        private val widget = widget
        private val caughtErrorSignal = caughtErrorSignal

        override fun isDisposed(): Boolean {
            return disposed
        }

        override fun dispose() {
            disposed = true

            try {
                widget.unbindModel()

                println("${DomainConst.TAG}: Unbind widget [$widget] " +
                        "from the model")
            } catch (err: Throwable) {
                caughtErrorSignal?.onNext(err)
            }
        }
    }
}
