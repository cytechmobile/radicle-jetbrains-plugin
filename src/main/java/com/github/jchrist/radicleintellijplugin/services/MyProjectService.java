package com.github.jchrist.radicleintellijplugin.services;

import com.intellij.openapi.project.Project;
import com.github.jchrist.radicleintellijplugin.MyBundle;

public class MyProjectService {

    public MyProjectService(Project project) {
        System.out.println(MyBundle.message("projectService", project.getName()));
    }
}
