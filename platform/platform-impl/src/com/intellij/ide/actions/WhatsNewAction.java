// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.experimental.ExperimentalUiCollector;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.IdeUrlTrackingParametersProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.platform.ide.customization.ExternalProductResourceUrls;
import com.intellij.ui.ExperimentalUI;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;

public final class WhatsNewAction extends AnAction implements DumbAware {
  private static final String ENABLE_NEW_UI_REQUEST = "enable-new-UI";

  @Override
  public void update(@NotNull AnActionEvent e) {
    var available = ExternalProductResourceUrls.getInstance().getWhatIsNewPageUrl() != null;
    e.getPresentation().setEnabledAndVisible(available);
    if (available) {
      e.getPresentation().setText(IdeBundle.messagePointer("whats.new.action.custom.text", ApplicationNamesInfo.getInstance().getFullProductName()));
      e.getPresentation().setDescription(IdeBundle.messagePointer("whats.new.action.custom.description", ApplicationNamesInfo.getInstance().getFullProductName()));
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    var whatsNewUrl = ExternalProductResourceUrls.getInstance().getWhatIsNewPageUrl();
    if (whatsNewUrl == null) throw new IllegalStateException();
    var url = whatsNewUrl.toExternalForm();

    if (ApplicationManager.getApplication().isInternal() && (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
      var title = IdeBundle.message("whats.new.action.custom.text", ApplicationNamesInfo.getInstance().getFullProductName());
      var prompt = IdeBundle.message("browser.url.popup");
      url = Messages.showInputDialog(e.getProject(), prompt, title, null, url, null);
      if (url == null) return;
    }

    var project = e.getProject();
    if (project != null && JBCefApp.isSupported()) {
//      openWhatsNewPage(project, url);
    }
    else {
      BrowserUtil.browse(IdeUrlTrackingParametersProvider.getInstance().augmentUrl(url));
    }
  }

  @ApiStatus.Internal
  public static void openWhatsNewPage(@NotNull Project project, @NotNull String url) {

  }
}