package com.appdsn.pullrefreshdemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appdsn.pullrefreshlayout.DefaultFooterView;
import com.appdsn.pullrefreshlayout.DefaultHeaderView;
import com.appdsn.pullrefreshlayout.PullRefreshLayout;
import com.appdsn.pullrefreshlayout.PullRefreshLayout.OnRefreshListener;

import java.util.ArrayList;

public class RecyclerViewActivity extends Activity {

    private ArrayList<String> filmListDatas;
    private RecyclerView listFilm;
    private PullRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);
        filmListDatas = new ArrayList<String>();
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        listFilm = (RecyclerView) findViewById(R.id.listFilm);
        listFilm.setLayoutManager(new
                StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        for (int i = 0; i < 50; i++) {
            filmListDatas.add(i + "");
        }

        listFilm.setAdapter(new Adapter<ViewHolder>() {

            @Override
            public int getItemCount() {
                // TODO Auto-generated method stub
                return filmListDatas.size();
            }

            @Override
            public void onBindViewHolder(ViewHolder arg0, int arg1) {
                // TODO Auto-generated method stub
                TextView textView = (TextView) arg0.itemView;
                textView.setPadding(20, 20, 20, 20);
                textView.setBackgroundColor(Color.WHITE);
                textView.setText(arg1 + "");
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup arg0, int arg1) {
                // TODO Auto-generated method stub

                return new ViewHolder(new TextView(RecyclerViewActivity.this)) {

                };
            }
        });

        refreshLayout.setFooterView(new DefaultFooterView(this), PullRefreshLayout.POSITION_FRONT);

        refreshLayout.setHeaderView(new DefaultHeaderView(this), PullRefreshLayout.POSITION_FRONT);
        refreshLayout.autoRefresh();
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
                }, 2000);
            }

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
        });


    }


}
