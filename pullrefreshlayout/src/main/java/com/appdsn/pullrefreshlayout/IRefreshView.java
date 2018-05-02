package com.appdsn.pullrefreshlayout;

import android.view.View;

public interface IRefreshView {
    /*拉动到什么高度位置后，松手开始刷新，默认是RefreshView的高度*/
    int getStartRefreshDistance();

    /*具体要添加到头部或尾部的View*/
    View getRefreshView();

    /*手指刚开始拉动时回调，初始化一些数据*/
    void onStart(PullRefreshLayout pullRefreshLayout);

    /*拉动完成后回调*/
    void onComplete(PullRefreshLayout pullRefreshLayout, boolean hasMoreData);

    /*手指不停的拉动过程中回调*/
    void onPull(PullRefreshLayout pullRefreshLayout, float percent);

    /*手指松开后，正在刷新或加载中回调*/
    void onRefresh(PullRefreshLayout pullRefreshLayout);
}
