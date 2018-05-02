package com.appdsn.pullrefreshdemo.refreshview;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.appdsn.pullrefreshdemo.R;
import com.appdsn.pullrefreshlayout.IRefreshView;
import com.appdsn.pullrefreshlayout.PullRefreshLayout;


/**
 * Created by wbz360 on 2017/4/1.
 */

public class CustomHeaderView implements IRefreshView {
    private AnimationDrawable animDrawable;
    private ImageView imgAnim;
    private TextView tvTitle;
    private String mStartText = "下拉刷新";
    private String mReleaseText = "松开刷新";
    private String mLoadingText = "正在刷新";
    private String mFinishText = "刷新完毕";
    private View headerView;
    private Context mContext;

    public CustomHeaderView(Context context) {
        mContext = context;
        headerView = LayoutInflater.from(context).inflate(R.layout.layout_header_view, null);
        imgAnim = (ImageView) headerView.findViewById(R.id.ivAnim);
        tvTitle = (TextView) headerView.findViewById(R.id.tvTitle);
        animDrawable = (AnimationDrawable) imgAnim.getDrawable();

        /*设置布局尺寸*/
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(160));
        params.gravity = Gravity.CENTER;
        headerView.setLayoutParams(params);

    }

    @Override
    public int getStartRefreshDistance() {
        return dp2px(100);
    }

    @Override
    public View getRefreshView() {
        return headerView;
    }

    @Override
    public void onStart(PullRefreshLayout pullRefreshLayout) {
        tvTitle.setText(mStartText);
        animDrawable.setVisible(true, true);//重置起始帧
        imgAnim.setPivotX(imgAnim.getWidth() / 2);
        imgAnim.setPivotY(imgAnim.getHeight());
    }

    @Override
    public void onComplete(PullRefreshLayout pullRefreshLayout, boolean hasMoreData) {
        animDrawable.stop();
        tvTitle.setText(mFinishText);
    }

    @Override
    public void onPull(PullRefreshLayout pullRefreshLayout, float percent) {
        imgAnim.setScaleX(percent);
        imgAnim.setScaleY(percent);
        if (percent >= 1) {
            tvTitle.setText(mReleaseText);
        } else {
            tvTitle.setText(mStartText);
        }
    }

    @Override
    public void onRefresh(PullRefreshLayout pullRefreshLayout) {
        tvTitle.setText(mLoadingText);
        animDrawable.start();
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mContext.getResources().getDisplayMetrics());
    }
}
