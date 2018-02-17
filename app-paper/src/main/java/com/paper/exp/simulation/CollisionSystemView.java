package com.paper.exp.simulation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.paper.R;
import com.paper.exp.simulation.CollisionSystemContract.SimulationListener;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;

public class CollisionSystemView
    extends View
    implements CollisionSystemContract.View {

    // Rendering.
    private final Paint mParticlePaint = new Paint();
    private final Paint mTextPaint = new Paint();
    private float mTextSize = 0f;

    private SimulationListener mListener;

    public CollisionSystemView(Context context) {
        super(context);
    }

    public CollisionSystemView(Context context,
                               @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollisionSystemView(Context context,
                               @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mParticlePaint.setStyle(Paint.Style.FILL);
        mParticlePaint.setColor(Color.BLACK);

        mTextSize = context.getResources().getDimension(R.dimen.debug_text_size_1);

        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public void schedulePeriodicRendering(@NotNull SimulationListener listener) {
        mListener = listener;
        postInvalidate();
    }

    @Override
    public void unScheduleAll() {
        mListener = null;
    }

    @NotNull
    @Override
    public Paint getParticlePaint() {
        return mParticlePaint;
    }

    @NotNull
    @Override
    public Observable<Object> onClickBack() {
        return Observable.just((Object) 0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onMeasure(int widthSpec,
                             int heightSpec) {
        final int width = MeasureSpec.getSize(widthSpec);
        final int height = width;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mListener != null) {
            mListener.onUpdateSimulation(canvas);
            postInvalidate();
        }
    }

    @Override
    public void showToast(@NotNull String text) {
        // DUMMY.
    }

    @Override
    public void showText(@NotNull Canvas canvas,
                         @NotNull String text) {
        float x = mTextSize;
        float y = mTextSize;
        for (String line : text.split("\n")) {
            canvas.drawText(line, x, y, mTextPaint);
            y += mTextSize;
        }
    }

    @Override
    public int getCanvasWidth() {
        return getWidth();
    }

    @Override
    public int getCanvasHeight() {
        return getHeight();
    }
}