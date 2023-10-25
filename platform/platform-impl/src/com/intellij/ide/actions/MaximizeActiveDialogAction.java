// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.ScreenUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class MaximizeActiveDialogAction extends WindowAction {
  private static final String NORMAL_BOUNDS = "NORMAL_BOUNDS";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    @Nullable Component component = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT);
    Window window = ComponentUtil.getWindow(component);
    if (!(window instanceof JDialog)) return;
    doMaximize((JDialog)window);
  }

  public static void doMaximize(JDialog dialog) {
    if (canBeMaximized(dialog)) {
      maximize(dialog);
    }
    else if (canBeNormalized(dialog)) {
      normalize(dialog);
    }
  }

  public static boolean canBeMaximized(JDialog dialog) {
    JRootPane rootPane = dialog != null && dialog.isResizable() ? dialog.getRootPane() : null;
    if (rootPane == null) return false;
    return !ScreenUtil.getScreenRectangle(dialog).equals(dialog.getBounds());
  }

  public static void maximize(JDialog dialog) {
    if (!canBeMaximized(dialog)) return;
    dialog.getRootPane().putClientProperty(NORMAL_BOUNDS, dialog.getBounds());
    dialog.setBounds(ScreenUtil.getScreenRectangle(dialog));
  }

  public static boolean canBeNormalized(JDialog dialog) {
    JRootPane rootPane = dialog != null && dialog.isResizable() ? dialog.getRootPane() : null;
    if (rootPane == null) return false;
    Rectangle screenRectangle = ScreenUtil.getScreenRectangle(dialog);
    return dialog.getBounds().equals(screenRectangle) && rootPane.getClientProperty(NORMAL_BOUNDS) instanceof Rectangle;
  }

  public static void normalize(JDialog dialog) {
    if (!canBeNormalized(dialog)) return;
    JRootPane rootPane = dialog.getRootPane();
    Object value = rootPane.getClientProperty(NORMAL_BOUNDS);
    if (value instanceof Rectangle bounds) {
      ScreenUtil.fitToScreen(bounds);
      dialog.setBounds(bounds);
      rootPane.putClientProperty(NORMAL_BOUNDS, null);
    }
  }


  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
