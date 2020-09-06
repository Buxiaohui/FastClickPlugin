/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclickplugin.fdc;

import java.util.concurrent.ConcurrentHashMap;

import android.os.SystemClock;
import android.util.LruCache;
import androidx.annotation.NonNull;

/**
 * 快速点击判断工具类
 * <p>
 * Created by tanglianghui on 2020/5/22 11:31 上午
 */
public class FastClickUtils {

    private static final String DEFAULT_TAG = "fast_click_default_tag";
    private static final long DEFAULT_TIME_INTERVAL = 800;

    private static final Object lock = new Object();
    private static final FastClickLruCache fastClickTimeMap = new FastClickLruCache(64);
    private static final ConcurrentHashMap<String, Long> backupMap = new ConcurrentHashMap<>();

    public static boolean isFastClick() {
        return isFastClick(DEFAULT_TAG, DEFAULT_TIME_INTERVAL);
    }

    public static boolean isFastClick(long timeInterval) {
        return isFastClick(DEFAULT_TAG, timeInterval);
    }

    public static boolean isFastClick(@NonNull String tag) {
        return isFastClick(tag, DEFAULT_TIME_INTERVAL);
    }

    public static boolean isFastClick(@NonNull String tag, long timeInterval) {
        long curTime = SystemClock.elapsedRealtime();
        synchronized(lock) {
            Long lastTime = fastClickTimeMap.get(tag);
            if (lastTime == null) {
                lastTime = backupMap.remove(tag);
            }
            boolean isFastClick = lastTime != null && (curTime - lastTime <= timeInterval);
            if (!isFastClick) {
                fastClickTimeMap.put(tag, curTime);
            }
            return isFastClick;
        }
    }

    static class FastClickLruCache extends LruCache<String, Long> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public FastClickLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Long oldValue, Long newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            if (!evicted) {
                return;
            }

            if (oldValue != null && key != null) {
                long curTime = SystemClock.elapsedRealtime();
                if (curTime - oldValue > 20000) {
                    backupMap.clear();
                } else {
                    backupMap.put(key, oldValue);
                }
            }
        }
    }
}
