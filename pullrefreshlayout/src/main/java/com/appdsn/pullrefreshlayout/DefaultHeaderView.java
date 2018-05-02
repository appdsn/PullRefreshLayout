package com.appdsn.pullrefreshlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by wbz360 on 2017/3/29.
 */

public class DefaultHeaderView extends View implements IRefreshView {
    /*圆弧相关*/
    private RectF mBounds;//圆弧框
    private int mCircleWidth;//圆弧宽
    private int mCircleHeight;//圆弧高
    private float mAngle;//弧线角度
    private int mLeft;//圆弧框位置
    private int mTop;//圆弧框位置
    private float mDegress;//旋转角度
    private boolean isRunning;
    /*绘制文本相关*/
    private Paint mTextPaint;
    private Paint.FontMetrics fontMetrics;
    private float baseX;//文字基准点坐标
    private float baseY;
    private String mText = "下拉刷新";
    /*箭头相关*/
    private boolean isShowArrow = true;//是否显示箭头
    private boolean isArrowTop = false;//箭头方向
    private int mArrowDegrees;//箭头旋转角度
    private boolean isRotateArrow = false;// 是否旋转箭头
    /*对号相关*/
    private boolean isShowHook = false;
    /*其他相关*/
    private Paint mPaint;
    private Path mPath;
    private int mDefaultWidth;//View宽
    private int mDefaultHeight;//View高

    public DefaultHeaderView(Context context) {
        this(context, null);
    }

    public DefaultHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(dp2px(1.5f));
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(Color.DKGRAY);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(dp2px(16));
        mTextPaint.setColor(Color.DKGRAY);
        mTextPaint.setTextAlign(Paint.Align.CENTER);//基点从中间开始
        fontMetrics = mTextPaint.getFontMetrics();

        mPath = new Path();
        mCircleWidth = dp2px(28);
        mCircleHeight = mCircleWidth;
        mDefaultWidth = dp2px(170);
        mDefaultHeight = dp2px(50);

        /*设置显示位置*/
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mHeight = resolveSize(mDefaultHeight, heightMeasureSpec);
        if (mHeight < mDefaultHeight) {
            mHeight = mDefaultHeight;
        }
        setMeasuredDimension(mDefaultWidth, mHeight);

        mTop = (getMeasuredHeight() - mCircleHeight) / 2;//垂直居中
        mLeft = dp2px(20);//靠左显示
        mBounds = new RectF(mLeft, mTop, mLeft + mCircleWidth, mTop + mCircleHeight);

        baseX = (getMeasuredWidth() - dp2px(50)) / 2 + dp2px(50);
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        baseY = (getMeasuredHeight() - textHeight) / 2 - fontMetrics.top;//垂直居中显示
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawRing(canvas);//画圆环
        drawArrow(canvas);//画箭头
        drawText(canvas);//画文本
        drawHook(canvas);//画对号
    }

    private void drawText(Canvas canvas) {
        canvas.save();
        canvas.drawText(mText, baseX, baseY, mTextPaint);
        canvas.restore();
    }

    private void drawRing(Canvas canvas) {
        canvas.save();
        canvas.rotate(mDegress, mBounds.centerX(), mBounds.centerY());
        mPath.reset();
        mPath.arcTo(mBounds, 280, mAngle, true);//280从最上面12点方向偏10度开始画
        canvas.drawPath(mPath, mPaint);
        canvas.restore();
        if (isRunning) {
            if (mDegress >= 360) {
                mDegress = 0;
            }
            mDegress += 10;
            invalidate();
        }
    }

    private void drawArrow(Canvas canvas) {
        if (isShowArrow) {
            canvas.save();
            /*控制箭头旋转*/
            canvas.rotate(mArrowDegrees, mBounds.centerX(), mBounds.centerY());
             /*单纯画一个箭头*/
            drawArrow(canvas, mLeft + mCircleWidth / 2, mTop + dp2px(4), mLeft + mCircleWidth / 2, mTop + mCircleHeight - dp2px(4), 40, 30);
            if (isRotateArrow) {
                if (isArrowTop) {
                    mArrowDegrees -= 15;
                    if (mArrowDegrees <= -180) {
                        mArrowDegrees = -180;
                        isRotateArrow = false;
                    }
                    invalidate();// 这里实现动画效果
                } else {
                    mArrowDegrees += 15;
                    if (mArrowDegrees >= 0) {
                        mArrowDegrees = 0;
                        isRotateArrow = false;
                    }
                    invalidate();// 这里实现动画效果
                }
            }
            canvas.restore();
        }

    }

    private void drawHook(Canvas canvas) {
        // 画对号
        if (isShowHook) {
            canvas.save();
            mPath.reset();
            mPath.moveTo(mBounds.centerX() - mCircleWidth / 2 + dp2px(4), mBounds.centerY());
            mPath.lineTo(mBounds.centerX() - dp2px(2), mBounds.centerY() + mCircleWidth / 2 - dp2px(8));
            mPath.lineTo(mBounds.centerX() + mCircleWidth / 2 - dp2px(6), mBounds.centerY() - dp2px(6));
            canvas.drawPath(mPath, mPaint);
            canvas.restore();
        }
    }

    @Override
    public int getStartRefreshDistance() {
        return 0;
    }

    @Override
    public View getRefreshView() {
        return this;
    }

    @Override
    public void onStart(PullRefreshLayout pullRefreshLayout) {
        mDegress = 0;
        mAngle = 0;
        isShowArrow = true;
        mArrowDegrees = 0;
        isShowHook = false;
        isRotateArrow = false;
        isArrowTop = false;
        mText = "下拉刷新";
        isRunning = false;
        invalidate();
    }

    @Override
    public void onComplete(PullRefreshLayout pullRefreshLayout, boolean hasMoreData) {
        isRunning = false;
        mDegress = 0;
        isShowArrow = false;
        isShowHook = true;
        mText = "刷新完毕";
        invalidate();
    }

    @Override
    public void onPull(PullRefreshLayout pullRefreshLayout, float percent) {
        mAngle = 340 * percent;
        if (percent >= 1) {
            mText = "松开刷新";
            if (!isArrowTop) {
                this.isArrowTop = true;
                isRotateArrow = true;
            }
        } else {//朝下
            mText = "下拉刷新";
            if (isArrowTop) {
                this.isArrowTop = false;
                isRotateArrow = true;
            }

        }
        invalidate();
    }

    @Override
    public void onRefresh(PullRefreshLayout pullRefreshLayout) {
        isRunning = true;
        isShowArrow = false;
        isShowHook = false;
        mText = "正在刷新...";
        invalidate();
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    /*fromX, fromY ：起点坐标
    toX, toY ：终点坐标
    theta ：三角斜边一直线夹角
    headlen ：三角斜边长度*/
    private void drawArrow(Canvas canvas, float fromX, float fromY, float toX, float toY, int theta, float headlen) {
        // 计算各角度和对应的P2,P3坐标
        double angle = Math.atan2(fromY - toY, fromX - toX) * 180 / Math.PI;
        double angle1 = (angle + theta) * Math.PI / 180;
        double angle2 = (angle - theta) * Math.PI / 180;
        double topX = headlen * Math.cos(angle1);
        double topY = headlen * Math.sin(angle1);
        double botX = headlen * Math.cos(angle2);
        double botY = headlen * Math.sin(angle2);
        /*开始画*/
        Path path = new Path();
        float arrowX = (float) (fromX - topX);
        float arrowY = (float) (fromY - topY);
        path.moveTo(arrowX, arrowY);
        path.moveTo(fromX, fromY);
        path.lineTo(toX, toY);
        arrowX = (float) (toX + topX);
        arrowY = (float) (toY + topY);
        path.moveTo(arrowX, arrowY);
        path.lineTo(toX, toY);
        arrowX = (float) (toX + botX);
        arrowY = (float) (toY + botY);
        path.lineTo(arrowX, arrowY);
        canvas.drawPath(path, mPaint);
    }

    private int evaluate(float fraction, int startValue, int endValue) {
        int startInt = startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }
}
