package com.github.jchrist.radicleintellijplugin.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.github.jchrist.radicleintellijplugin.services.MyProjectService;

public class MyProjectManagerListener implements ProjectManagerListener {

    @Override
    public void projectOpened(Project project) {
        var srv = project.getService(MyProjectService.class);
        System.out.println("got service: " + srv);
    }
}
