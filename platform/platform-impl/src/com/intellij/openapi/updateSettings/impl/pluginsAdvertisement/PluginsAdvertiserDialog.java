// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.updateSettings.impl.pluginsAdvertisement;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.PluginNode;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBDimension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PluginsAdvertiserDialog extends DialogWrapper {
  private final @Nullable Project myProject;
  private final @NotNull List<PluginNode> myCustomPlugins;
  private final @Nullable Consumer<Boolean> myFinishFunction;
  private final boolean mySelectAllSuggestions;

  PluginsAdvertiserDialog(@Nullable Project project,
                          @NotNull List<PluginNode> customPlugins,
                          boolean selectAllSuggestions,
                          @Nullable Consumer<Boolean> finishFunction) {
    super(project);
    myProject = project;
    myCustomPlugins = customPlugins;
    myFinishFunction = finishFunction;
    mySelectAllSuggestions = selectAllSuggestions;
    setTitle(IdeBundle.message("dialog.title.choose.plugins.to.install.or.enable"));
    init();

    JRootPane rootPane = getPeer().getRootPane();
    if (rootPane != null) {
      rootPane.setPreferredSize(new JBDimension(800, 600));
    }
  }

  public PluginsAdvertiserDialog(@Nullable Project project,
                                 @NotNull List<PluginNode> customPlugins) {
    this(project, customPlugins, false, null);
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  protected void doOKAction() {
//    assert myPanel != null;
//    if (doInstallPlugins(myPanel::isChecked, ModalityState.stateForComponent(myPanel))) {
//      super.doOKAction();
//    }
  }

  /**
   * @param showDialog if the dialog will be shown to a user or not
   * @param modalityState modality state used by plugin installation process.
   *                      {@code modalityState} will taken into account only if {@code showDialog} is <code>false</code>.
   *                      If {@code null} is passed, {@code ModalityState.NON_MODAL} will be used
   */
  public void doInstallPlugins(boolean showDialog, @Nullable ModalityState modalityState) {
    if (showDialog) {
      showAndGet();
    }
  }


}
