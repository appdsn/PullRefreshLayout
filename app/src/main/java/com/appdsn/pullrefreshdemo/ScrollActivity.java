package com.appdsn.pullrefreshdemo;

import android.app.Activity;
import android.os.Bundle;

import com.appdsn.pullrefreshlayout.DefaultFooterView;
import com.appdsn.pullrefreshlayout.PullRefreshLayout;


public class ScrollActivity extends Activity {

    private PullRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll);

        refreshLayout = (PullRefreshLayout)
                findViewById(R.id.refreshLayout);

        refreshLayout.setFooterView(new DefaultFooterView(this), PullRefreshLayout.POSITION_TOP);
        refreshLayout.setHeaderPosition(PullRefreshLayout.POSITION_TOP);
        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onLoadMore(PullRefreshLayout pullRefreshLayout) {
                pullRefreshLayout.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        refreshLayout.finishLoadMore(true);
                    }
                }, 2000);
            }

            @Override
            public void onRefresh(PullRefreshLayout pullRefreshLayout) {
                // TODO Auto-generated method stub
                pullRefreshLayout.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        refreshLayout.finishRefresh();
                    }
                }, 2000);
            }
        });

        refreshLayout.autoRefresh();
    }
}
