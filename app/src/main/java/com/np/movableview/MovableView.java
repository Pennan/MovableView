package com.np.movableview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * 功能：
 * ①、可随手指的移动而移动
 * ②、点击的时候只响应点击事件，滑动的时候只响应滑动事件
 * ③、进行边界控制，只允许在屏幕内移动.
 */
public class MovableView extends View {
    private static final String TAG = "MovableView";
    private int mTouchSlop; // 系统认为的最小滑动距离
    private int screenWidth;
    private int screenHeight;

    public MovableView(Context context) {
        this(context, null);
    }

    public MovableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MovableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        // metrics.heightPixels(屏幕高度) = 屏幕空白处高度 + 顶部状态栏(不包括底部导航栏)
        screenHeight = metrics.heightPixels - getStatusHeight();
    }

    int lastX;
    int lastY;
    int rawX;
    int rawY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int currX = (int) event.getRawX();
        int currY = (int) event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                rawX = lastX = currX;
                rawY = lastY = currY;
                break;
            case MotionEvent.ACTION_MOVE:
                int offsetX = currX - lastX;
                int offsetY = currY - lastY;
                moveView(offsetX, offsetY);
                lastX = currX;
                lastY = currY;
                break;
            case MotionEvent.ACTION_UP:
                // 处理点击事件和移动事件
                if (Math.abs(lastX - rawX) < mTouchSlop &&
                        Math.abs(lastY - rawY) < mTouchSlop) {
                    performClick();
                }
                break;
        }

        return true;
    }

    private void moveView(int offsetX, int offsetY) {
        // 方法一：
        /*ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
        int left = params.leftMargin + offsetX;
        int top = params.topMargin + offsetY;

        params.leftMargin = left;
        params.topMargin = top;
        setLayoutParams(params);
        requestLayout();*/
        // 方法二：
        /*offsetLeftAndRight(offsetX);
        offsetTopAndBottom(offsetY);*/
        // 方法三：
        /*RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
        int left = params.leftMargin + offsetX;
        int top = params.topMargin + offsetY;

        params.leftMargin = left;
        params.topMargin = top;
        setLayoutParams(params);
        requestLayout();*/
        // 方法四：
        /*((View) getParent()).scrollBy(-offsetX, -offsetY);*/
        // 方法五：做了屏幕边界控制
        int left = getLeft() + offsetX;
        int top = getTop() + offsetY;
        int right = getRight() + offsetX;
        int bottom = getBottom() + offsetY;
        if (left < 0 || right > screenWidth) {
            left = getLeft() - offsetX;
            right = getRight() - offsetX;
        }
        if (top < 0 || bottom > screenHeight) {
            top = getTop() - offsetY;
            bottom = getBottom() - offsetY;
        }
        layout(left, top, right, bottom);
    }

    private int getStatusHeight() {
        int statusBarHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusBarHeight = getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }
}
