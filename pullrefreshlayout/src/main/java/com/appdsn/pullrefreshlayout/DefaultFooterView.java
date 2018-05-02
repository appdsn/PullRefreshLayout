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

public class DefaultFooterView extends View implements IRefreshView {
    private static final int MAX_LEVEL = 200;

    private boolean isRunning;
    /*圆弧相关*/
    private RectF mBounds;//圆弧框
    private int mCircleWidth;//圆弧宽
    private int mCircleHeight;//圆弧高
    private float mAngle;//弧线角度
    private int mLeft;//圆弧框位置
    private int mTop;//圆弧框位置
    private float mCircleDegress;//旋转角度
    private float mSpeedCount;
    /*绘制文本相关*/
    private Paint mTextPaint;
    private Paint.FontMetrics fontMetrics;
    private float baseX;//文字基准点坐标
    private float baseY;
    private String mStartText = "上拉加载";
    private String mReleaseText = "松开加载";
    private String mLoadingText = "正在加载...";
    private String mFinishText = "加载完毕";
    private String mNodataText = "没有更多数据";
    private String mText = mStartText;
    /*箭头相关*/
    private boolean isShowArrow = true;//是否显示箭头
    private boolean isArrowTop = true;//箭头方向
    private int mArrowDegrees;//箭头旋转角度
    private boolean isRotateArrow = false;// 是否旋转箭头
    /*对号相关*/
    private boolean isShowCircle = false;
    /*其他相关*/
    private int[] mColorSchemeColors;
    private int mLevel;
    private Paint mPaint;
    private Path mPath;
    private int mMiniWidth;//View宽
    private int mMiniHeight;//View高

    public DefaultFooterView(Context context) {
        this(context, null);
    }

    public DefaultFooterView(Context context, AttributeSet attrs) {
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
        mCircleWidth = dp2px(30);
        mCircleHeight = mCircleWidth;
        mMiniHeight = dp2px(50);

        /*设置显示位置*/
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /*测量文字的长度*/
        int textWidth = (int) mTextPaint.measureText(mText);
        mMiniWidth = (textWidth / 2 + mCircleWidth + dp2px(20)) * 2;//为了文字能居中显示
        int mWidth = resolveSize(mMiniWidth, widthMeasureSpec);
        if (mWidth < mMiniWidth) {//防止设置太小
            mWidth = mMiniWidth;
        }
        int mHeight = resolveSize(mMiniHeight, heightMeasureSpec);
        if (mHeight < mMiniHeight) {
            mHeight = mMiniHeight;
        }
        setMeasuredDimension(mWidth, mHeight);

        mTop = (getMeasuredHeight() - mCircleHeight) / 2;//垂直居中
        mLeft = (getMeasuredWidth() - textWidth) / 2 - mCircleWidth - dp2px(20);//靠文字左边20dp显示
        mBounds = new RectF(mLeft, mTop, mLeft + mCircleWidth, mTop + mCircleHeight);

        baseX = getMeasuredWidth() / 2;
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        baseY = (getMeasuredHeight() - textHeight) / 2 - fontMetrics.top;//垂直居中显示
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawText(canvas);//画文本
        drawCircle(canvas);//画圆环
        drawArrow(canvas);//画箭头
    }

    private void drawText(Canvas canvas) {
        canvas.save();
        canvas.drawText(mText, baseX, baseY, mTextPaint);
        canvas.restore();
    }

    private void drawCircle(Canvas canvas) {
        if (isShowCircle) {
            canvas.save();
            canvas.rotate(mCircleDegress, mBounds.centerX(), mBounds.centerY());
            mSpeedCount++;
            if (mSpeedCount == 3) {
                mCircleDegress = mCircleDegress < 360 ? mCircleDegress + 30 : 0;
                mSpeedCount = 0;
            }

            // 画刻度共12个，每次旋转360/12=30
            for (int i = 0; i < 12; i++) {
                int alpha = (i + 1) * 255 / 11;
                mPaint.setAlpha(alpha);
                canvas.drawLine(mBounds.centerX(), mBounds.centerY() - mCircleHeight / 3, mBounds.centerX(), mBounds.centerY()
                        - mCircleWidth / 6, mPaint);
                // 通过旋转画布简化坐标运算
                canvas.rotate(30, mBounds.centerX(), mBounds.centerY());
            }
            canvas.restore();
            if (isRunning) {
                invalidate();
            }
        }
    }

    private void drawArrow(Canvas canvas) {
        if (isShowArrow) {
            canvas.save();
            /*控制箭头旋转*/
            canvas.rotate(mArrowDegrees, mBounds.centerX(), mBounds.centerY());
             /*单纯画一个箭头*/
            drawArrow(canvas, mLeft + mCircleWidth / 2, mBounds.bottom - dp2px(4), mLeft + mCircleWidth / 2, mTop + dp2px(4), 40, 30);
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

    /*默认是当前View的高度*/
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
        mCircleDegress = 0;
        isShowArrow = true;
        mArrowDegrees = 0;
        isShowCircle = false;
        isRotateArrow = false;
        isArrowTop = true;
        mText = mStartText;
        isRunning = false;
        mPaint.setColor(Color.DKGRAY);
        invalidate();
    }

    @Override
    public void onComplete(PullRefreshLayout pullRefreshLayout, boolean hasMoreData) {
        if (hasMoreData) {
            isRunning = false;
            mText = mFinishText;
        } else {
            isShowArrow = false;
            isShowCircle = false;
            mText = mNodataText;
        }
    }

    @Override
    public void onPull(PullRefreshLayout pullRefreshLayout, float percent) {
        if (percent >= 1) {
            mText = mReleaseText;
            if (!isArrowTop) {
                this.isArrowTop = true;
                isRotateArrow = true;
            }
        } else {//朝下
            mText = mStartText;
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
        isShowCircle = true;
        mText = mLoadingText;
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

    public void setTextColor(int textColor) {
        mTextPaint.setColor(textColor);
    }

    public void setCircleColor(int circleColor) {
        mPaint.setColor(circleColor);
    }

    public void setStartText(String startText) {
        mStartText = startText;
    }

    public void setReleaseText(String releaseText) {
        mReleaseText = releaseText;
    }

    public void setLoadingText(String loadingText) {
        mLoadingText = loadingText;
    }

    public void setFinishText(String finishText) {
        mFinishText = finishText;
    }

    public void setNoDataText(String nodataText) {
        mNodataText = nodataText;
    }
}
