package com.mosect.draglayout.example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mosect.draglayout.entity.ListItem;
import com.mosect.draglayout.entity.PageEntity;
import com.mosect.draglayout.lib.VerticalRefreshLayout;
import com.mosect.draglayout.model.ListDataModel;
import com.mosect.draglayout.util.WorkCallback;
import com.mosect.draglayout.util.WorkRunnable;
import com.mosect.draglayout.widget.PagerView;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PagerViewFragment extends Fragment {

    private int currentPageNumber = 0;
    private ListAdapter contentAdapter;
    private PagerView pvContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pagerview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pvContent = view.findViewById(R.id.pv_content);
        pvContent.setBackgroundColor(Color.parseColor("#303030"));
        pvContent.getContent().setBackgroundColor(Color.parseColor("#ffffff"));
        contentAdapter = new ListAdapter();
        contentAdapter.initWith(pvContent.getContent());
        pvContent.setLoadCallback(new VerticalRefreshLayout.LoadCallback() {
            @Override
            public void onRefresh(VerticalRefreshLayout view) {
                loadPage(1, true);
            }

            @Override
            public void onLoadMore(VerticalRefreshLayout view) {
                loadPage(currentPageNumber + 1, false);
            }
        });

        final TextView tvEmpty = view.findViewById(R.id.tv_empty);
        contentAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                tvEmpty.setVisibility(contentAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
            }
        });

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pvContent.postRefresh(true);
    }

    /**
     * 加载分页数据
     *
     * @param page    页数
     * @param refresh 是否为刷新操作
     */
    private void loadPage(final int page, final boolean refresh) {
        Observable<PageEntity<ListItem>> observable = Observable.create(new WorkRunnable<PageEntity<ListItem>>() {
            @Override
            protected PageEntity<ListItem> doWork() throws Throwable {
                return ListDataModel.getInstance().loadPage(page);
            }
        });
        observable.observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new WorkCallback<PageEntity<ListItem>>() {
                    @Override
                    protected void onSuccess(PageEntity<ListItem> result) {
                        loadPageSuccess(result, refresh);
                    }
                });
    }

    private void loadPageSuccess(PageEntity<ListItem> data, boolean refresh) {
        if (null != data) {
            currentPageNumber = data.getPageNumber();
        }

        // 添加数据
        if (refresh) {
            contentAdapter.clear();
        }
        if (null != data && null != data.getList()) {
            contentAdapter.addAll(data.getList());
        }

        int count = null != data && null != data.getList() ? data.getList().size() : 0;
        boolean hasNextPage = null != data && (data.getPageNumber() - 1) * data.getPageSize() +
                count < data.getTotal();
        pvContent.finishLoad(hasNextPage);
        pvContent.postDelayed(new Runnable() {
            @Override
            public void run() {
                pvContent.closeEdge(true);
            }
        }, 500);
    }
}
