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

package com.paper.domain.widget.editor

import android.graphics.Bitmap
import com.paper.domain.ISharedPreferenceService
import com.paper.domain.event.ProgressEvent
import com.paper.domain.event.UndoRedoEvent
import com.paper.domain.useCase.BindWidgetWithModel
import com.paper.domain.useCase.SavePaperToStore
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class PaperEditorWidget(paperRepo: IPaperRepo,
                        sharedPrefs: ISharedPreferenceService,
                        penPrefs: ICommonPenPrefsRepo,
                        caughtErrorSignal: Observer<Throwable>,
                        uiScheduler: Scheduler,
                        ioScheduler: Scheduler) {

    private val mPaperRepo = paperRepo
    private val mSharedPrefs = sharedPrefs
    private val mPenPrefs = penPrefs
    private val mCaughtErrorSignal = caughtErrorSignal

    private val mUiScheduler = uiScheduler
    private val mIoScheduler = ioScheduler

    private val mDisposables = CompositeDisposable()

    fun start(paperID: Long) {
        ensureNoLeakingSubscription()

        // Load paper and bind paper widget with the paper
        val paperSrc = mPaperRepo
            .getPaperById(paperID)
            .toObservable()
            .publish()
        val paperBindingSrc = paperSrc
            .switchMap { paper ->
                BindWidgetWithModel(
                    widget = mPaperWidget,
                    model = paper,
                    caughtErrorSignal = mCaughtErrorSignal)
                    .subscribeOn(mUiScheduler)
                    .toObservable()
            }
        val historyBindingSrc = paperSrc
            .switchMap { paper ->
                BindWidgetWithModel(
                    widget = mHistoryWidget,
                    model = paper,
                    caughtErrorSignal = mCaughtErrorSignal)
                    .subscribeOn(mUiScheduler)
                    .toObservable()
            }
        mDisposables.add(
            Observables
                .zip(paperBindingSrc,
                     historyBindingSrc)
                .map { (result1, result2) -> result1 && result2 }
                .observeOn(mUiScheduler)
                .subscribe { done ->
                    if (done) {
                        mOnPaperWidgetReadySignal.onNext(mPaperWidget)
                    }
                })
        paperSrc.connect()

        // Bind the edit panel widget with the pen preference model
        mDisposables.add(
            BindWidgetWithModel(
                widget = mEditPanelWidget,
                model = mPenPrefs)
                .observeOn(mUiScheduler)
                .subscribe { done ->
                    if (done) {
                        mOnEditPanelWidgetReadySignal.onNext(mEditPanelWidget)
                    }
                })

        // Following are all about the edit panel outputs to paper widget:

        // Choose what drawing tool
        mDisposables.add(
            mEditPanelWidget
                .onUpdateEditToolList()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
//                    val toolID = event.toolIDs[event.usingIndex]
//                    mPaperWidget.handleChooseTool()
                })

        // Pen colors
        mDisposables.add(
            mEditPanelWidget
                .onUpdatePenColorList()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    val color = event.colorTickets[event.usingIndex]
                    mPaperWidget.handleChoosePenColor(color)
                })
        // Pen size
        mDisposables.add(
            mEditPanelWidget
                .onUpdatePenSize()
                .observeOn(mUiScheduler)
                .subscribe { penSize ->
                    mPaperWidget.handleUpdatePenSize(penSize)
                })

        // Following are about undo and redo:
        mDisposables.add(
            Observables
                .combineLatest(
                mHistoryWidget.onUpdateUndoCapacity(),
                mHistoryWidget.onUpdateRedoCapacity())
                .map { (undo, redo) ->
                    UndoRedoEvent(canUndo = undo > 0,
                                  canRedo = redo > 0)
                }
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    mUndoRedoEventSignal.onNext(event)
                })
    }

    fun stop() {
        mDisposables.dispose()
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already bind to a widget")
    }

    // Paper widget ///////////////////////////////////////////////////////////

    private val mPaperWidget by lazy {
        PaperWidget(mUiScheduler,
                    mIoScheduler)
    }

    private val mOnPaperWidgetReadySignal = PublishSubject.create<IPaperWidget>()

    // TODO: The interface is probably redundant
    fun onPaperWidgetReady(): Observable<IPaperWidget> {
        return mOnPaperWidgetReadySignal
    }

    fun writePaperToRepo(bmpSrc: Single<Bitmap>): Single<Boolean> {
        return bmpSrc.compose(SavePaperToStore(
            paper = mPaperWidget.getPaper(),
            paperRepo = mPaperRepo,
            prefs = mSharedPrefs,
            caughtErrorSignal = mCaughtErrorSignal))
    }

    // Edit panel widget //////////////////////////////////////////////////////

    private val mOnEditPanelWidgetReadySignal = PublishSubject.create<PaperEditPanelWidget>()

    private val mEditPanelWidget by lazy {
        PaperEditPanelWidget(
            uiScheduler = mUiScheduler,
            workerScheduler = mIoScheduler)
    }

    fun onEditPanelWidgetReady(): Observable<PaperEditPanelWidget> {
        return mOnEditPanelWidgetReadySignal
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val mHistoryWidget by lazy {
        HistoryWidget()
    }

    private val mUndoRedoEventSignal = BehaviorSubject.createDefault(
        UndoRedoEvent(canUndo = false,
                      canRedo = false))

    fun handleUndo() {
        if (!mUndoRedoEventSignal.value.canUndo) return

        // TODO
    }

    fun handleRedo() {
        if (!mUndoRedoEventSignal.value.canRedo) return

        // TODO
    }

    fun onGetUndoRedoEvent(): Observable<UndoRedoEvent> {
        return mUndoRedoEventSignal
    }

    // Progress & error & Editor status ///////////////////////////////////////

    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    fun onUpdateProgress(): Observable<ProgressEvent> {
        return mUpdateProgressSignal
    }

    fun onGetStatus(): Observable<Any> {
        TODO()
    }
}
