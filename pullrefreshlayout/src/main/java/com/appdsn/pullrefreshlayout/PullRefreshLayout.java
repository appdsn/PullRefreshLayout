package com.appdsn.pullrefreshlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.util.ArrayList;

/*需要改进的地方：刷新和上拉同时进行的冲突*/
public class PullRefreshLayout extends FrameLayout {

    /*HeaderView和FooterView的位置：后面，上面，覆盖*/
    public static final int POSITION_BACK = 1;
    public static final int POSITION_TOP = 2;
    public static final int POSITION_FRONT = 3;
    /*HeaderView和FooterView的状态：可拉动，正在刷新，刷新完成，没有更多数据*/
    private static final int STATUS_PULL = 1;
    private static final int STATUS_REFRESH = 2;
    private static final int STATUS_COMPLETE = 3;
    private static final int STATUS_NO_DATA = 4;

    // *************************************************************************
    private View mContentView;// 内容布局
    private OnRefreshListener refreshListener;
    /*HeaderView相关设置*/
    private FrameLayout mHeaderViewLayout;// 头部布局
    private IRefreshView mHeaderView;// 头部显示的view
    private int mHeaderPosition = POSITION_TOP;
    private boolean isRefreshEnable = true;
    private float mStartRefreshDistance = 0;// 头部下拉多少高度释放后会刷新，默认是headerView高度
    private int headerStatus = STATUS_COMPLETE;
    /*FooterView相关设置*/
    private FrameLayout mFooterViewLayout;// 尾部布局
    private IRefreshView mFooterView;// 尾部显示的view
    private int mFooterPosition = POSITION_TOP;
    private boolean isLoadMoreEnable = true;
    private float mStartLoadMoreDistance = 0;// 头部下拉多少高度释放后会刷新，默认是footerView高度
    private boolean isAutoLoadEnable = false;//是否支持滑动到底部自动加载更多
    private boolean hasMoreData = true;//是否有更多数据
    private int footerStatus = STATUS_COMPLETE;
    /*核心逻辑相关*/
    private boolean mDispatchTargetTouchDown = false;
    private float xLast;
    private float yLast;
    private float xFirstPoint;
    private float yFirstPoint;
    private float offsetRadio = 2.0f;// 阻尼系数
    /*记录触摸点，支持多点触控*/
    private ArrayList<Integer> pointIds = new ArrayList<>();
    private int pointId;
    /*默认RefreshView*/
    private DefaultHeaderView mDefaultHeaderView;
    private DefaultFooterView mDefaultFooterView;
    private ValueAnimator headerAnimator;
    private ValueAnimator footerAnimator;

    public PullRefreshLayout(Context context) {
        this(context, null);
    }

    /* 初始化一些属性数据，此时view还没有被画出来，构�?�时view布局中的子view还没有被添加进来，getChildCount==0 */
    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (isInEditMode()) {//解决可视化编辑器无法识别自定义控件的问题
            return;
        }
        mHeaderViewLayout = new FrameLayout(context);
        mFooterViewLayout = new FrameLayout(context);
        mDefaultHeaderView = new DefaultHeaderView(context);
        mDefaultFooterView = new DefaultFooterView(context);
    }

    /*
     * 初始化view，该方法在onMeasure之前，在构造方法以后执行。
     * 所以，这里要注意：不能在该方法之前使用child view
     */
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        int count = getChildCount();
        if (count != 1) {
            throw new RuntimeException("can only have one child widget");
        }
        mContentView = getChildAt(0);
         /*内容布局总是和最外层宽高相同*/
        LayoutParams layoutParams = (LayoutParams) mContentView.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        // 这里给头部添加一个FrameLayout，主要是为了扩展性考虑，后面可以自定义头部view
        LayoutParams layoutParams1 = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (mHeaderPosition == POSITION_BACK) {
            addView(mHeaderViewLayout, 0, layoutParams1);//叠在一起时，显示在mContentView后面
        } else {
            addView(mHeaderViewLayout, layoutParams1);
        }

        LayoutParams layoutParams2 = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (mFooterPosition == POSITION_BACK) {
            addView(mFooterViewLayout, 0, layoutParams2);
        } else {
            addView(mFooterViewLayout, layoutParams2);
        }
        /*默认是有上拉和下拉View的，如果要控制下拉或上拉是否可用，请使用isRefreshEnable,isLoadMoreEnable*/
        if (mHeaderView == null) {
            setHeaderView(mDefaultHeaderView, mHeaderPosition);
        }
        if (mFooterView == null) {
            setFooterView(mDefaultFooterView, mFooterPosition);
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeView(mHeaderViewLayout);
        removeView(mFooterViewLayout);
    }

    /*为了支持padding和Margin，所以继承自Framlayout，复用系统测量方法代码*/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mStartRefreshDistance <= 0) {
            mStartRefreshDistance = mHeaderViewLayout.getMeasuredHeight();
        }

        if (mStartLoadMoreDistance <= 0) {
            mStartLoadMoreDistance = mFooterViewLayout.getMeasuredHeight();
        }
    }

    /*自己布局，注意要和onMeasure配套，也要支持padding和Margin才行*/
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int parentLeft = getPaddingLeft();
        final int parentRight = right - left - getPaddingRight();
        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        LayoutParams lp;
        int width;
        int height;
        int childLeft;
        int childTop;

        /*布局contentView*/
        lp = (LayoutParams) mContentView.getLayoutParams();
        width = mContentView.getMeasuredWidth();
        height = mContentView.getMeasuredHeight();
        childLeft = parentLeft + lp.leftMargin;
        childTop = parentTop + lp.topMargin;
        mContentView.layout(childLeft, childTop, childLeft + width, childTop + height);

        /*布局headView*/
        lp = (LayoutParams) mHeaderViewLayout.getLayoutParams();
        width = mHeaderViewLayout.getMeasuredWidth();
        height = mHeaderViewLayout.getMeasuredHeight();
        if (mHeaderPosition == POSITION_BACK) {
            childLeft = parentLeft + lp.leftMargin;
            childTop = parentTop + lp.topMargin;
        } else {
            childLeft = parentLeft + lp.leftMargin;
            childTop = parentTop + lp.topMargin - height;
        }
        mHeaderViewLayout.layout(childLeft, childTop, childLeft + width, childTop + height);

         /*布局footView*/
        lp = (LayoutParams) mFooterViewLayout.getLayoutParams();
        width = mFooterViewLayout.getMeasuredWidth();
        height = mFooterViewLayout.getMeasuredHeight();
        if (mFooterPosition == POSITION_BACK) {
            childLeft = parentLeft + lp.leftMargin;
            childTop = parentBottom - lp.bottomMargin - height;
        } else {
            childLeft = parentLeft + lp.leftMargin;
            childTop = parentBottom - lp.bottomMargin;
        }
        mFooterViewLayout.layout(childLeft, childTop, childLeft + width, childTop + height);
    }

    /*该方法一旦拦截，将会调用onTouchEvent方法，后续不会再执行onInterceptTouchEvent方法了*/
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN://不拦截down事件，如果被子View拦截了，只能通过move事件拦截
                /*初始化一些参数*/
                mDispatchTargetTouchDown = false;
                xFirstPoint = ev.getX();
                yFirstPoint = ev.getY();
                pointId = ev.getPointerId(ev.getActionIndex());
                pointIds.add(pointId);
                /*初始化头部和尾部View的状态*/
                if (headerStatus == STATUS_COMPLETE) {
                    headerStatus = STATUS_PULL;
                    mHeaderView.onStart(this);//首次先调用onStart
                }
                if (footerStatus == STATUS_COMPLETE && hasMoreData) {//正常状态下并且有更多数据，才会初始化状态
                    footerStatus = STATUS_PULL;
                    mFooterView.onStart(this);
                }

                /*停止正在执行的动画*/
                if (headerAnimator != null && headerAnimator.isStarted()) {
                    headerAnimator.removeAllListeners();
                    headerAnimator.cancel();
                }
                if (footerAnimator != null && footerAnimator.isStarted()) {
                    footerAnimator.removeAllListeners();
                    footerAnimator.cancel();
                }
                break;
            case MotionEvent.ACTION_MOVE://拦截move事件
                /*做一些参数效验*/
                if (mContentView == null) {
                    return false;
                }

                int mTouchSlop = ViewConfiguration.get(getContext())
                        .getScaledTouchSlop();
                float deltaY = ev.getY() - yFirstPoint;
                float absDiff = Math.abs(deltaY);// 取绝对值
                if (absDiff < mTouchSlop) {//垂直方向防误触
                    return false;
                }

                xLast = ev.getX();//初始化开始位置，防止突然下拉一定距离（TouchSlop距离）
                yLast = ev.getY();
                return true;//拦截后，立即交给onTouchEvent处理
        }
        return super.onInterceptTouchEvent(ev);//对于Action_down事件不拦截
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN://防止ContentView没有消耗Down事件（导致后续事件不会分发来）
                xLast = e.getX();// 初始化开始位置，防止突然下拉一定距离（TouchSlop距离）
                yLast = e.getY();
                return true;//此时，必须消耗down事件，否则后续事件不会分发来
            case MotionEvent.ACTION_POINTER_DOWN://以新手指为基准获取坐标
                pointId = e.getPointerId(e.getActionIndex());
                pointIds.add(pointId);
                xLast = e.getX(e.getActionIndex());
                yLast = e.getY(e.getActionIndex());
                return true;
            case MotionEvent.ACTION_MOVE:
                float curX = e.getX(e.findPointerIndex(pointId));
                float curY = e.getY(e.findPointerIndex(pointId));
                float dx = curX - xLast;
                float dy = curY - yLast;
//                float xDistance = Math.abs(dx);//解决左右滑动冲突
//                float yDistance = Math.abs(dy);
                xLast = curX;
                yLast = curY;
                float offsetY = dy / offsetRadio;
                offsetY = (float) Math.rint(offsetY);//转换成整数：四舍五入，不到1个像素就不会处理
                if (offsetY == 0) {
                    return true;
                }
                pullTransChildrenView(offsetY, e);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                pointIds.clear();//重置触摸点id
                autoTransChildrenView(e);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                /*移除抬起的触摸点id，并将事件坐标设置为最后一个触摸点*/
                pointId = e.getPointerId(e.getActionIndex());
                int index = pointIds.indexOf(pointId);
                if (index >= 0 && index < pointIds.size()) {
                    pointIds.remove(index);
                }
                pointId = pointIds.get(pointIds.size() - 1);
                xLast = e.getX(e.findPointerIndex(pointId));
                yLast = e.getY(e.findPointerIndex(pointId));
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void dispatchTouchEventToChild(final MotionEvent e, float offsetY) {
        if (mDispatchTargetTouchDown) {
            mContentView.dispatchTouchEvent(e);
        } else {
            MotionEvent event = MotionEvent.obtain(e);
            /* 为了防止触发按下的效果*/
            if (offsetY > 0) {//下拉
                e.setLocation(e.getX(), e.getY() - ViewConfiguration.get(getContext())
                        .getScaledTouchSlop() - offsetY);//加减一个像素点，为了满足滑动大于TouchSlop
            } else if (offsetY < 0) {//上拉
                e.setLocation(e.getX(), e.getY() + ViewConfiguration.get(getContext())
                        .getScaledTouchSlop() - offsetY);
            }
            e.setAction(MotionEvent.ACTION_DOWN);
            mContentView.dispatchTouchEvent(e);

            /*立即给它一个Move事件，防止按键事件发生*/
            event.setAction(MotionEvent.ACTION_MOVE);
            mContentView.dispatchTouchEvent(event);
            event.recycle();

            mDispatchTargetTouchDown = true;
        }
    }

    private boolean isHeadViewPullOut() {
        boolean isOut = false;
        float y = getHeaderTransY();
        if (y > 0) {
            isOut = true;
        }
        return isOut;
    }

    private boolean isFootViewPullOut() {
        boolean isOut = false;
        float y = getFooterTransY();
        if (y < 0) {
            isOut = true;
        }
        return isOut;
    }

    /*头部和尾部View同时只能拉动一个*/
    private void pullTransChildrenView(float offsetY, MotionEvent e) {
        if (isHeadViewPullOut()) {
            //拉动mHeadViewLayout
            pullHeaderLayout(getHeaderTransY() + offsetY, e);
        } else if (isFootViewPullOut()) {
            //拉动mFootViewLayout
            pullFooterLayout(getFooterTransY() + offsetY, e);
        } else {
             /*下面根据不同的位置来处理不同的逻辑*/
            if (offsetY < 0) {//上拉:分2种情况，拉出尾部或者滚动内容区
                if (!canChildScrollDown() && isLoadMoreEnable) {//控制上拉加载是否可用
                    //拉出mFootViewLayout
                    if (isAutoLoadEnable) {//支持自动加载的逻辑判断
                        setLoadingMore();
                    }
                    pullFooterLayout(getFooterTransY() + offsetY, e);
                } else {
                    dispatchTouchEventToChild(e, offsetY);
                }

            } else if (offsetY > 0) {//下拉:分2种情况，拉出头部或者滚动内容区
                if (!canChildScrollUp() && isRefreshEnable) {//控制下拉刷新是否可用
                    //拉出mHeadViewLayout
                    pullHeaderLayout(getHeaderTransY() + offsetY, e);
                } else {
                    dispatchTouchEventToChild(e, offsetY);
                }
            }
        }
    }

    /*在当前位置的基础上，移动头部布局*/
    protected void pullHeaderLayout(float mHeadOffsetY, MotionEvent e) {
        if (mDispatchTargetTouchDown) {//此处取消子View的事件，防止下次事件再发给改子View时抖动一个距离
            e.setAction(MotionEvent.ACTION_CANCEL);
            mContentView.dispatchTouchEvent(e);
            mDispatchTargetTouchDown = false;
        }

        // 这里很重要，不然快速滑动时mCurrentOffset不会等于0
        if (mHeadOffsetY < 0) {
            dispatchTouchEventToChild(e, mHeadOffsetY); //剩余的距离分发给子view处理
            mHeadOffsetY = 0;
        }
        float percent = mHeadOffsetY / mStartRefreshDistance;// 下拉高度的百分比
        if (percent > 1) {
            percent = 1;
        }
        if (mHeaderView != null && headerStatus == STATUS_PULL) {//不在刷新时才会调用（可以根据需要修改此处逻辑）
            mHeaderView.onPull(PullRefreshLayout.this, percent);
        }

        if (mHeaderPosition == POSITION_BACK) {
            //3.0后提供的平移方法(相对于原来位置平移多少，不带动画，大于0向下平移)
            mContentView.setTranslationY(mHeadOffsetY);

        } else if (mHeaderPosition == POSITION_FRONT) {
            mHeaderViewLayout.setTranslationY(mHeadOffsetY);

        } else {
            mHeaderViewLayout.setTranslationY(mHeadOffsetY);
            mContentView.setTranslationY(mHeadOffsetY);
        }

    }

    protected void pullFooterLayout(float mFootOffsetY, MotionEvent e) {
        if (mDispatchTargetTouchDown) {
            MotionEvent obtain = MotionEvent.obtain(e);
            obtain.setAction(MotionEvent.ACTION_CANCEL);
            mContentView.dispatchTouchEvent(obtain);
            obtain.recycle();
            mDispatchTargetTouchDown = false;
        }

        // 这里很重要，不然快速下滑动时mFootOffsetY不会等于0
        if (mFootOffsetY > 0) {
            dispatchTouchEventToChild(e, mFootOffsetY); //剩余的距离分发给子view处理
            mFootOffsetY = 0;
        }
        float percent = -mFootOffsetY / mStartLoadMoreDistance;// 下拉高度的百分比
        if (percent > 1) {
            percent = 1;
        }
        if (mFooterView != null && footerStatus == STATUS_PULL && hasMoreData) {// 不在刷新且有数据时才会调用
            mFooterView.onPull(PullRefreshLayout.this, percent);
        }

        if (mFooterPosition == POSITION_BACK) {
            mContentView.setTranslationY(mFootOffsetY);
        } else if (mFooterPosition == POSITION_FRONT) {
            mFooterViewLayout.setTranslationY(mFootOffsetY);
        } else {
            mFooterViewLayout.setTranslationY(mFootOffsetY);
            mContentView.setTranslationY(mFootOffsetY);
        }
    }

    /*手指离开屏幕时候，会利用属性动画自动平滑滚动View到合适的位置*/
    private void autoTransChildrenView(MotionEvent e) {
        if (mDispatchTargetTouchDown) {
            dispatchTouchEventToChild(e, 0);
        }
        if (isHeadViewPullOut()) {
            float mStartValue = getHeaderTransY();
            if (mStartValue >= mStartRefreshDistance) {
                animTransHeaderView(mStartValue, mStartRefreshDistance, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        setRefreshing();
                    }
                });

            } else {
                animTransHeaderView(mStartValue, 0, null);
            }

        } else if (isFootViewPullOut()) {
            float mStartValue = getFooterTransY();
            if (-mStartValue >= mStartLoadMoreDistance) {
                animTransFooterView(mStartValue, -mStartLoadMoreDistance, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        setLoadingMore();
                    }
                });

            } else {
                animTransFooterView(mStartValue, 0, null);
            }
        }
    }

    /*得到当前的平移量*/
    private float getHeaderTransY() {
        float curTransY = 0;
        if (mHeaderPosition == POSITION_BACK) {//只移动contentview
            curTransY = mContentView.getTranslationY();
        } else if (mHeaderPosition == POSITION_FRONT) {//只移动headerview
            curTransY = mHeaderViewLayout.getTranslationY();
        } else {//两者都移动
            curTransY = mHeaderViewLayout.getTranslationY();
        }
        return curTransY;
    }

    private float getFooterTransY() {
        float curTransY = 0;
        if (mFooterPosition == POSITION_BACK) {//只移动contentview
            curTransY = mContentView.getTranslationY();
        } else if (mFooterPosition == POSITION_FRONT) {//只移动footererview
            curTransY = mFooterViewLayout.getTranslationY();
        } else {//两者都移动
            curTransY = mFooterViewLayout.getTranslationY();
        }
        return curTransY;
    }

    /*属性动画移动View到指定位置*/
    private void animTransFooterView(float startValue, float endValue, Animator.AnimatorListener animListener) {
        View[] views = null;
        if (mFooterPosition == POSITION_BACK) {//只移动contentview
            views = new View[]{mContentView};
        } else if (mFooterPosition == POSITION_FRONT) {//只移动footerview
            views = new View[]{mFooterViewLayout};
        } else {//两者都移动
            views = new View[]{mContentView, mFooterViewLayout};
        }
        final View[] transViews = views;

        if (footerAnimator == null) {
            footerAnimator = new ValueAnimator();
        }
        footerAnimator.setFloatValues(startValue, endValue);
        footerAnimator.setDuration(300);
        footerAnimator.removeAllListeners();
        footerAnimator.removeAllUpdateListeners();
        if (animListener != null) {
            footerAnimator.addListener(animListener);
        }

        footerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                float value = (Float) animator.getAnimatedValue();
                for (int i = 0; i < transViews.length; i++) {
                    transViews[i].setTranslationY(value);
                }

                float percent = -value / mStartLoadMoreDistance;//上拉高度的百分比
                if (percent > 1) {
                    percent = 1;
                }
                if (mFooterView != null && footerStatus == STATUS_PULL && hasMoreData) {// 不在加载时才会调用
                    mFooterView.onPull(PullRefreshLayout.this, percent);
                }
            }
        });
        footerAnimator.start();
    }

    private void animTransHeaderView(float startValue, float endValue, Animator.AnimatorListener animListener) {
        View[] views = null;
        if (mHeaderPosition == POSITION_BACK) {//只移动contentview
            views = new View[]{mContentView};
        } else if (mHeaderPosition == POSITION_FRONT) {//只移动headerview
            views = new View[]{mHeaderViewLayout};
        } else {//两者都移动
            views = new View[]{mContentView, mHeaderViewLayout};
        }
        final View[] transViews = views;

        if (headerAnimator == null) {
            headerAnimator = new ValueAnimator();
        }
        headerAnimator.setFloatValues(startValue, endValue);
        headerAnimator.setDuration(300);
        headerAnimator.removeAllListeners();
        headerAnimator.removeAllUpdateListeners();
        if (animListener != null) {
            headerAnimator.addListener(animListener);
        }
        headerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                float value = (Float) animator.getAnimatedValue();
                for (int i = 0; i < transViews.length; i++) {
                    transViews[i].setTranslationY(value);
                }

                 /*下面为了autoRefesh时的拉动效果*/
                float percent = value / mStartRefreshDistance;//上拉高度的百分比
                if (percent > 1) {
                    percent = 1;
                }
                if (mHeaderView != null && headerStatus == STATUS_PULL) {// 不在加载时才会调用
                    mHeaderView.onPull(PullRefreshLayout.this, percent);
                }
            }
        });
        headerAnimator.start();
    }

    /*手指松开后，自动滑动View到指定位置的动画结束后才会刷新*/
    private void setRefreshing() {
        if (headerStatus != STATUS_REFRESH) {//已经在刷新就不需要再次刷新了
            headerStatus = STATUS_REFRESH;
            if (mHeaderView != null) {
                mHeaderView.onRefresh(PullRefreshLayout.this);
            }
            if (refreshListener != null) {
                refreshListener.onRefresh(PullRefreshLayout.this);
            }
        }
    }

    private void setLoadingMore() {
        if (footerStatus != STATUS_REFRESH && hasMoreData) {
            footerStatus = STATUS_REFRESH;
            if (mFooterView != null) {
                mFooterView.onRefresh(PullRefreshLayout.this);
            }
            if (refreshListener != null) {
                refreshListener.onLoadMore(PullRefreshLayout.this);
            }
        }
    }

    /*************************下面是用户可以动态设置的各种参数**************************/

    public void setHeaderView(IRefreshView headerView) {
        setHeaderView(headerView, POSITION_TOP, null);
    }

    public void setHeaderView(IRefreshView headerView, int position) {
        setHeaderView(headerView, position, null);
    }

    public void setHeaderView(IRefreshView headerView, int position, LayoutParams layoutParams) {
        if (headerView == null || headerView.getRefreshView() == null) {
            return;
        }
        mHeaderView = headerView;
        mHeaderPosition = position;
        mStartRefreshDistance = headerView.getStartRefreshDistance();
        mHeaderViewLayout.removeAllViews();

        if (layoutParams != null) {
            mHeaderViewLayout.addView(headerView.getRefreshView(), layoutParams);
        } else {
            mHeaderViewLayout.addView(headerView.getRefreshView());
        }
    }


    public void setFooterView(IRefreshView footView) {
        setFooterView(footView, POSITION_TOP, null);
    }

    public void setFooterView(IRefreshView footView, int position) {
        setFooterView(footView, position, null);
    }

    public void setFooterView(IRefreshView footView, int position, LayoutParams layoutParams) {
        if (footView == null || footView.getRefreshView() == null) {
            return;
        }
        mFooterPosition = position;
        mFooterView = footView;
        mStartLoadMoreDistance = footView.getStartRefreshDistance();
        mFooterViewLayout.removeAllViews();

        if (layoutParams != null) {
            mFooterViewLayout.addView(footView.getRefreshView(), layoutParams);
        } else {
            mFooterViewLayout.addView(footView.getRefreshView());
        }
    }

    public DefaultHeaderView getDefaultHeaderView() {
        return mDefaultHeaderView;
    }

    public DefaultFooterView getDefaultFooterView() {
        return mDefaultFooterView;
    }

    public void setHeaderPosition(int headerPosition) {
        this.mHeaderPosition = headerPosition;
    }

    public void setFooterPosition(int footPosition) {
        this.mFooterPosition = footPosition;
    }

    public void setStartRefreshDistance(float refreshDistance) {
        this.mStartRefreshDistance = refreshDistance;
    }

    public void setStartLoadMoreDistance(float loadMoreDistance) {
        this.mStartLoadMoreDistance = loadMoreDistance;
    }

    public void setOffsetRadio(float offsetRadio) {
        this.offsetRadio = offsetRadio;
    }

    public void setRefreshEnable(boolean enable) {
        isRefreshEnable = enable;
    }

    public void setLoadMoreEnable(boolean enable) {
        isLoadMoreEnable = enable;
    }

    public void setAutoLoadEnable(boolean enable) {
        isAutoLoadEnable = enable;
    }

    public void autoRefresh() {
        autoRefresh(500);
    }

    public void autoRefresh(long delayTime) {
        if (headerStatus != STATUS_REFRESH) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mContentView == null) {
                        return;
                    }
                    if (headerStatus == STATUS_COMPLETE) {
                        mHeaderView.onStart(PullRefreshLayout.this);
                        headerStatus = STATUS_PULL;
                    }

                    animTransHeaderView(0, mStartRefreshDistance, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            setRefreshing();
                        }
                    });

                }
            }, delayTime);
        }
    }

    public void finishLoadMore(boolean hasMoreData) {
        if (mContentView != null && footerStatus == STATUS_REFRESH) {
            this.hasMoreData = hasMoreData;
            if (mFooterView != null) {
                mFooterView.onComplete(this, hasMoreData);
            }

            postDelayed(new Runnable() {
                @Override
                public void run() {
                    footerStatus = STATUS_COMPLETE;
                    animTransFooterView(getFooterTransY(), 0, null);
                }
            }, 200);
        }
    }

    public void finishRefresh() {
        if (mContentView != null && headerStatus == STATUS_REFRESH) {
            if (mHeaderView != null) {
                mHeaderView.onComplete(this, true);
            }

            /*展示刷新成功的标志200毫秒后，自动平滑隐藏View：隐藏动画过程中不可以拉动，结束后才能拉动*/
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    headerStatus = STATUS_COMPLETE;
                    animTransHeaderView(getHeaderTransY(), 0, null);
                }
            }, 200);

            /*重置footerView的状态数据*/
            hasMoreData = true;
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    public static interface OnRefreshListener {
        public void onRefresh(PullRefreshLayout pullRefreshLayout);

        public void onLoadMore(PullRefreshLayout pullRefreshLayout);
    }

    /**
     * 小于0表示是否向上滚动了一定距离，0初始位置不包括在内
     * true只表示向上移动了一定距离
     */
    private boolean canChildScrollUp() {
        if (mContentView == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (mContentView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mContentView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView
                        .getChildAt(0).getTop() < absListView
                        .getPaddingTop());
            } else {
                return mContentView.canScrollVertically(-1)
                        || mContentView.getScrollY() > 0;
            }
        } else {
            return mContentView.canScrollVertically(-1);
        }
    }

    /**
     * 大于等于零，是否还能向上移动，true表示还能向上移动，false表示到底部了
     */
    private boolean canChildScrollDown() {
        if (mContentView == null) {
            return false;
        }
        return mContentView.canScrollVertically(1);
    }

    private int dip2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
