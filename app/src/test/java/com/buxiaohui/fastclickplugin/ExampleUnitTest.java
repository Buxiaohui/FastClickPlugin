/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclickplugin;

import com.buxiaohui.fastclickplugin.fdc.FastClickUtils;

public class ExampleUnitTest {

    public void onClick() {
        if (FastClickUtils.isFastClick()) {
            return;
        }

        if (FastClickUtils.isFastClick("haha")) {
            return;
        }

        if (FastClickUtils.isFastClick("haha", 999)) {
            return;
        }

        if (FastClickUtils.isFastClick(111)) {
            return;
        }
    }
}