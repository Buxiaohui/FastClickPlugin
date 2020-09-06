package com.buxiaohui.fastclick

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project;

class FastClickPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project != null) {
            AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
            appExtension.registerTransform(new FastClickTransform());
        }
    }
}