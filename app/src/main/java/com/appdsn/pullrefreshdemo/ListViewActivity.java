package com.appdsn.pullrefreshdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.appdsn.pullrefreshdemo.refreshview.CustomHeaderView;
import com.appdsn.pullrefreshlayout.DefaultFooterView;
import com.appdsn.pullrefreshlayout.PullRefreshLayout;
import com.appdsn.pullrefreshlayout.PullRefreshLayout.OnRefreshListener;


import java.util.ArrayList;

public class ListViewActivity extends Activity {

    private ArrayList<String> listDatas;
    private ListView listFilm;
    private PullRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);
        Log.i("123", "onCreate");
        listDatas = new ArrayList<String>();
        Log.i("123", "refreshLayout0");
        refreshLayout = (PullRefreshLayout)
                findViewById(R.id.refreshLayout);
        Log.i("123", "refreshLayout0");


        listFilm = (ListView) findViewById(R.id.listFilm);

        for (int i = 0; i < 50; i++) {
            listDatas.add(i + "");
        }
        listFilm.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listDatas));

        refreshLayout.setHeaderView(new CustomHeaderView(this), PullRefreshLayout.POSITION_TOP);
//        refreshLayout.setRefreshEnable(true);
//        refreshLayout.setLoadMoreEnable(true);
//        refreshLayout.setAutoLoadEnable(false);
//        refreshLayout.setHeaderPosition(PullRefreshLayout.POSITION_TOP);
//        refreshLayout.setFooterPosition(PullRefreshLayout.POSITION_TOP);
//        refreshLayout.setOffsetRadio(2.0f);
//        refreshLayout.setStartRefreshDistance(100);
//        refreshLayout.setStartLoadMoreDistance(100);

        refreshLayout.setFooterView(new DefaultFooterView(this), PullRefreshLayout.POSITION_TOP);

        refreshLayout.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh(PullRefreshLayout pullRefreshLayout) {
                // TODO Auto-generated method stub
                pullRefreshLayout.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        refreshLayout.finishRefresh();

                    }
                }, 3000);
            }

            @Override
            public void onLoadMore(PullRefreshLayout pullRefreshLayout) {
                pullRefreshLayout.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        refreshLayout.finishLoadMore(true);
                    }
                }, 3000);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("123", "onResume");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("123", "onResume");
    }
}
