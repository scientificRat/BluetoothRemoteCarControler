package com.scientificrat.stm32.bluetoothremotecarcontroler.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by huangzhengyue on 2016/10/25.
 */

public class Rocker extends View {
    private Paint pen = new Paint();

    private final float DEFAULT_RADIUS = 150;
    private final float DEFAULT_BAR_RADIUS = 150 / 2.5F;
    //输出的范围 [-RANGE,+RANGE]
    private final float RANGE = 1;

    private final float MAX_RADIUS = 220;

    private float radius = DEFAULT_RADIUS;
    private float barRadius = DEFAULT_BAR_RADIUS;
    private float width = DEFAULT_RADIUS;
    private float height = DEFAULT_RADIUS;

    private float barX = 200;
    private float barY = 200;

    private float lastBarX =200;
    private float lastBarY =200;

    private boolean justInit = true;

    private OnRockerChangeListener onRockerChangeListener = null;

    public Rocker(Context context) {
        super(context);
    }

    public Rocker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.width = this.getWidth();
        this.height = this.getHeight();
        if (justInit) {
            barX = this.width / 2;
            barY = this.height / 2;
            justInit = false;
        }
        if (width < height) {
            this.radius = Math.min(width / 2, MAX_RADIUS);
        } else {
            this.radius = Math.min(height / 2, MAX_RADIUS);
        }
        this.barRadius = radius / 2.5f;
        //绘制外周圆
        pen.setColor(Color.argb(255, 59, 63, 71));
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeWidth(7);
        canvas.drawCircle(width / 2, height / 2, this.radius - 4, pen);
        //绘制中心bar
        pen.setColor(Color.argb(200, 200, 200, 200));
        pen.setStyle(Paint.Style.FILL);
        canvas.drawCircle(barX, barY, this.barRadius, pen);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //抬起或者移出区域时恢复正常位置
        int action = event.getAction();
        if (MotionEvent.ACTION_UP == action || MotionEvent.ACTION_OUTSIDE == action) {
            barX = width / 2;
            barY = height / 2;

            lastBarX = barX;
            lastBarY = barY;
            //listener callback
            if (this.onRockerChangeListener != null) {
                onRockerChangeListener.onRockerChange(0, 0);
            }

        } else {
            final float x = event.getX();
            final float y = event.getY();

            //触摸点相对与中心的半径的平方 r_square
            float deltaX = (x - width / 2);
            float deltaY = (y - height / 2);
            float r_square = deltaX * deltaX + deltaY * deltaY;
            if (r_square <= this.radius * this.radius) {
                barX = x;
                barY = y;
            } else {
                float scaleFactor = this.radius / (float) Math.sqrt(r_square);
                barX = this.width / 2 + deltaX * scaleFactor;
                barY = this.height / 2 + deltaY * scaleFactor;
            }
            //listener callback (设置了一定的触发阈值)
            if (this.onRockerChangeListener != null && (Math.abs(barX-lastBarX)>=1.5 || Math.abs(barY-lastBarY)>=1.5 )) {
                onRockerChangeListener.onRockerChange((barX -width/2) / this.radius * RANGE, (barY-height/2) / this.radius * RANGE);
            }
            lastBarX = barX;
            lastBarY = barY;

        }
        //eat this event, so I do not call the super method
        //let it redraw
        invalidate();
        return true;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setOnRockerChangeListener(OnRockerChangeListener onRockerChangeListener) {
        this.onRockerChangeListener = onRockerChangeListener;
    }

    public float getRange() {
        return RANGE;
    }
}
