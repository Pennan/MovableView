package com.np.movableview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * 功能：
 * 1、可随手指的移动而移动
 * 2、无事件时，过段时间自动透明
 * 3、点击的时候只响应点击事件，滑动的时候只响应滑动事件
 * 4、进行边界控制，只允许在屏幕内移动.
 * 5、松开后自动回到屏幕左侧或右侧
 */
public class MovableView extends View {
    private static final String TAG = "MovableView";
    private int mTouchSlop; // 系统认为的最小滑动距离
    private int screenWidth;
    private int screenHeight;

    private MyCountDownTimer countDownTimer;
    private long millisInFuture = 2500;
    private long countDownInterval = 500;

    private float toAlpha = 0.2f;

    // 可以移动的范围
    private int minLeftMargin;
    private int maxLeftMargin;
    private int minTopMargin;
    private int maxTopMargin;

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

        countDownTimer = new MyCountDownTimer(millisInFuture, countDownInterval);
        countDownTimer.start();
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
                setAlpha(1f);
                countDownTimer.cancel();

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
                countDownTimer.start();
                // 处理点击事件和移动事件
                if (Math.abs(lastX - rawX) < mTouchSlop &&
                        Math.abs(lastY - rawY) < mTouchSlop) {
                    performClick();
                }

                // 停止移动时，使 View 贴边.
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) getLayoutParams();
                int fromLeftMargin = lp.leftMargin;
                if ((getLeft() + getMeasuredWidth() / 2) <= screenWidth / 2) {
                    lp.leftMargin = minLeftMargin;
                } else {
                    lp.leftMargin = maxLeftMargin;
                }
                ObjectAnimator marginChange = ObjectAnimator.ofInt(new Wrapper(this), "leftMargin", fromLeftMargin, lp.leftMargin);
                marginChange.setDuration(500);
                marginChange.start();
                break;
        }

        return true;
    }

    private void moveView(int offsetX, int offsetY) {
        // 方法一：
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
        int left = params.leftMargin + offsetX;
        int top = params.topMargin + offsetY;

        left = left < minLeftMargin ? minLeftMargin : left;
        left = left > maxLeftMargin ? maxLeftMargin : left;
        top = top < minTopMargin ? minTopMargin : top;
        top = top > maxTopMargin ? maxTopMargin : top;

        params.leftMargin = left;
        params.topMargin = top;
        setLayoutParams(params);
        requestLayout();
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
        /*int left = getLeft() + offsetX;
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
        layout(left, top, right, bottom);*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        minLeftMargin = 0;
        maxLeftMargin = screenWidth - getMeasuredWidth();
        minTopMargin = 0;
        maxTopMargin = screenHeight - getMeasuredHeight();
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

    /**
     * 使 View 能够执行属性动画的 包装类.
     */
    class Wrapper {
        private View mTarget;

        public Wrapper(View view) {
            mTarget = view;
        }

        public int getLeftMargin() {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTarget.getLayoutParams();
            return lp.leftMargin;
        }

        public void setLeftMargin(int leftMargin) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTarget.getLayoutParams();
            lp.leftMargin = leftMargin;
            mTarget.requestLayout();
        }
    }

    class MyCountDownTimer extends CountDownTimer {

        /**
         * @param millisInFuture    倒计时时间:
         *                          调用 start() 方法开始倒计时，经过 millisInFuture 秒时间(倒计时结束),
         *                          倒计时结束后调用 onFinish() 方法.
         * @param countDownInterval 在倒计时(millisInFuture)这段时间里,每隔 countDownInterval 段时间执行一次 onTick() 方法.
         */
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            setAlpha(toAlpha);
        }
    }
}
