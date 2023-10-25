// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsUtils;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehaviorSpecification;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ResetFontSizeActionBase extends EditorAction implements ActionRemoteBehaviorSpecification.Frontend {
  static final String UNSCALED_FONT_SIZE_TO_RESET_CONSOLE = "fontSizeToResetConsole";
  static final String UNSCALED_FONT_SIZE_TO_RESET_EDITOR = "fontSizeToResetEditor";
  public static final String PREVIOUS_COLOR_SCHEME = "previousColorScheme";
  private final boolean myGlobal;

  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }

  @ApiStatus.Internal
  public interface Strategy {
    float getFontSize();

    void setFontSize(float fontSize);

    @NlsActions.ActionText String getText(float fontSize);

    default void reset() {
      setFontSize(getFontSize());
    }
  }

  private static final class SingleEditorStrategy implements Strategy {
    private final EditorEx myEditorEx;

    SingleEditorStrategy(EditorEx editorEx) {
      myEditorEx = editorEx;
    }

    @Override
    public float getFontSize() {
      UISettingsUtils uiSettings = UISettingsUtils.getInstance();
      return 0.0f;//ConsoleViewUtil.isConsoleViewEditor(myEditorEx) ? uiSettings.getScaledConsoleFontSize() : uiSettings.getScaledEditorFontSize();
    }

    @Override
    public void setFontSize(float fontSize) {
      myEditorEx.setFontSize(fontSize);
    }

    @Override
    public String getText(float fontSize) {
      return IdeBundle.message("action.reset.font.size", fontSize);
    }
  }

  private static final class AllEditorsStrategy implements Strategy {
    private final EditorEx myEditorEx;

    AllEditorsStrategy(EditorEx editorEx) {
      myEditorEx = editorEx;
    }

    @Override
    public float getFontSize() {
      return UISettingsUtils.getInstance().scaleFontSize(getUnscaledFontSize());
    }

    private float getUnscaledFontSize() {
      PropertiesComponent propertyComponent = PropertiesComponent.getInstance();
//      if (ConsoleViewUtil.isConsoleViewEditor(myEditorEx)) {
//        return propertyComponent.getFloat(UNSCALED_FONT_SIZE_TO_RESET_CONSOLE, -1);
//      }
      return propertyComponent.getFloat(UNSCALED_FONT_SIZE_TO_RESET_EDITOR, -1);
    }

    @Override
    public void reset() {
      setFontSize(getUnscaledFontSize());
    }

    @Override
    public void setFontSize(float fontSize) {
      EditorColorsManager.getInstance().getGlobalScheme().setEditorFontSize(fontSize);
      ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorColorsManager.TOPIC).globalSchemeChange(null);
    }

    @Override
    public String getText(float fontSize) {
      return IdeBundle.message("action.reset.font.size.all.editors", fontSize);
    }
  }

  private static final class PresentationModeStrategy implements Strategy {
    @Override
    public float getFontSize() {
      return UISettingsUtils.getInstance().getPresentationModeFontSize();
    }

    @Override
    public void setFontSize(float fontSize) {
      int fs = (int)fontSize;
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        if (editor instanceof EditorEx) {
          ((EditorEx)editor).setFontSize(fs);
        }
      }
    }

    @Override
    public String getText(float fontSize) {
      return IdeBundle.message("action.reset.font.size", fontSize);
    }
  }

  @ApiStatus.Internal
  public static Strategy getStrategy(EditorEx editor, boolean forceGlobal) {
    if (editor instanceof EditorImpl) {
      if (forceGlobal) {
        return new AllEditorsStrategy(editor);
      }
      if (UISettings.getInstance().getPresentationMode()) {
        return new PresentationModeStrategy();
      }
      if (EditorSettingsExternalizable.getInstance().isWheelFontChangePersistent()) {
        return new AllEditorsStrategy(editor);
      }
    }
    return new SingleEditorStrategy(editor);
  }

  ResetFontSizeActionBase(boolean forceGlobal) {
    super(new MyHandler(forceGlobal));
    myGlobal = forceGlobal;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (e.getPlace().equals(ActionPlaces.POPUP) && editor != null) {
      if (!(editor instanceof EditorEx editorEx)) {
        return;
      }
      Strategy strategy = getStrategy(editorEx, myGlobal);
      float toReset = strategy.getFontSize();
      //noinspection DialogTitleCapitalization
      e.getPresentation().setText(strategy.getText(toReset));
      if (editor instanceof EditorImpl) {
        e.getPresentation().setEnabled(((EditorImpl)editor).getFontSize2D() != toReset);
      }
    }
  }

  private static final class MyHandler extends EditorActionHandler {
    private final boolean myGlobal;

    MyHandler(boolean forceGlobal) {
      super();
      myGlobal = forceGlobal;
    }

    @Override
    public void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      if (!(editor instanceof EditorEx)) {
        return;
      }
      getStrategy((EditorEx)editor, myGlobal).reset();
    }
  }
}
