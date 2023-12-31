// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.options;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ConfigurableBase<UI extends ConfigurableUi<S>, S> implements SearchableConfigurable, Configurable.NoScroll {
  private final String id;
  private final @NlsContexts.ConfigurableName String displayName;
  private final String helpTopic;

  private UI ui;

  protected ConfigurableBase(@NonNls @NotNull String id, @NotNull @NlsContexts.ConfigurableName String displayName, @NonNls @Nullable String helpTopic) {
    this.id = id;
    this.displayName = displayName;
    this.helpTopic = helpTopic;
  }

  @Override
  public final @NotNull String getId() {
    return id;
  }

  @Override
  public final String getDisplayName() {
    return displayName;
  }

  @Override
  public final @Nullable String getHelpTopic() {
    return helpTopic;
  }

  protected abstract @NotNull S getSettings();

  @Override
  public void reset() {
    if (ui != null) {
      ui.reset(getSettings());
    }
  }

  @Override
  public final @NotNull JComponent createComponent() {
    if (ui == null) {
      ui = createUi();
    }
    return ui.getComponent();
  }

  @Override
  public @Nullable Runnable enableSearch(String option) {
    return ui == null ? null : ui.enableSearch(option);
  }

  protected abstract UI createUi();

  @Override
  public final boolean isModified() {
    return ui != null && ui.isModified(getSettings());
  }

  @Override
  public final void apply() throws ConfigurationException {
    if (ui != null) {
      ui.apply(getSettings());
    }
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return ui != null ? ui.getPreferredFocusedComponent() : null;
  }

  @Override
  public void disposeUIResources() {
    UI ui = this.ui;
    if (ui != null) {
      this.ui = null;
      if (ui instanceof Disposable) {
        Disposer.dispose((Disposable)ui);
      }
    }
  }
}