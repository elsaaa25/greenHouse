package com.example.greenhouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class LineChartView extends View {

    private double[] data = new double[0];
    private Paint linePaint;
    private Paint pointPaint;
    private Paint popupPaint;
    private Paint textPaint;
    private Path path;
    
    private int selectedIndex = -1;
    private float lastTouchX = -1;
    private float lastTouchY = -1;

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#9575CD"));
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#9575CD"));
        pointPaint.setStyle(Paint.Style.FILL);

        popupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        popupPaint.setColor(Color.BLACK);
        popupPaint.setAlpha(180);
        popupPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        path = new Path();
    }

    public void setData(double[] data) {
        this.data = data;
        this.selectedIndex = -1;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            
            float drawWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            float drawHeight = getHeight() - getPaddingTop() - getPaddingBottom();
            float xStep = drawWidth / (data.length - 1);
            float yScale = drawHeight / 100f;

            int closestIndex = -1;
            float minDistance = 50f; // Threshold for touch

            for (int i = 0; i < data.length; i++) {
                float px = getPaddingLeft() + i * xStep;
                float py = getPaddingTop() + drawHeight - (float) (data[i] * yScale);
                float distance = (float) Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2));
                
                if (distance < minDistance) {
                    minDistance = distance;
                    closestIndex = i;
                }
            }
            
            selectedIndex = closestIndex;
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.length < 2) return;

        float drawWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float drawHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float xStep = drawWidth / (data.length - 1);
        float yScale = drawHeight / 100f;

        path.reset();

        for (int i = 0; i < data.length; i++) {
            float x = getPaddingLeft() + i * xStep;
            float y = getPaddingTop() + drawHeight - (float) (data[i] * yScale);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, linePaint);

        // Draw points and popup
        for (int i = 0; i < data.length; i++) {
            float x = getPaddingLeft() + i * xStep;
            float y = getPaddingTop() + drawHeight - (float) (data[i] * yScale);
            canvas.drawCircle(x, y, 10f, pointPaint);
            
            if (i == selectedIndex) {
                String valueStr = String.format("%.2f", data[i]);
                float textWidth = textPaint.measureText(valueStr);
                float popupPadding = 10f;
                float popupWidth = textWidth + popupPadding * 2;
                float popupHeight = 40f;
                
                float popupX = x + 20f;
                float popupY = y - 40f;
                
                // Adjust popup position if it goes off screen
                if (popupX + popupWidth > getWidth()) {
                    popupX = x - popupWidth - 20f;
                }
                
                // Draw simple popup background
                canvas.drawRoundRect(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 10f, 10f, popupPaint);
                // Draw text
                canvas.drawText(valueStr, popupX + popupWidth / 2, popupY + popupHeight - 10f, textPaint);
            }
        }
    }
}