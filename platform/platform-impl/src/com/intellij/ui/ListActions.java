// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehaviorSpecification;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ListActions extends SwingActionDelegate implements ActionRemoteBehaviorSpecification.Frontend {
  private ListActions(String actionId) {
    super(actionId);
  }

  @Override
  protected @Nullable JList<?> getComponent(AnActionEvent event) {
    var component = super.getComponent(event);
    return (JList<?>) component;
  }

  public static final class Home extends ListActions {
    public static final @NonNls String ID = "selectFirstRow";

    public Home() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftHome extends ListActions {
    public static final @NonNls String ID = "selectFirstRowExtendSelection";

    public ShiftHome() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class End extends ListActions {
    public static final @NonNls String ID = "selectLastRow";

    public End() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftEnd extends ListActions {
    public static final @NonNls String ID = "selectLastRowExtendSelection";

    public ShiftEnd() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class Up extends ListActions {
    public static final @NonNls String ID = "selectPreviousRow";

    public Up() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftUp extends ListActions {
    public static final @NonNls String ID = "selectPreviousRowExtendSelection";

    public ShiftUp() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class Down extends ListActions {
    public static final @NonNls String ID = "selectNextRow";

    public Down() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftDown extends ListActions {
    public static final @NonNls String ID = "selectNextRowExtendSelection";

    public ShiftDown() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class Left extends ListActions {
    public static final @NonNls String ID = "selectPreviousColumn";

    public Left() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftLeft extends ListActions {
    public static final @NonNls String ID = "selectPreviousColumnExtendSelection";

    public ShiftLeft() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class Right extends ListActions {
    public static final @NonNls String ID = "selectNextColumn";

    public Right() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftRight extends ListActions {
    public static final @NonNls String ID = "selectNextColumnExtendSelection";

    public ShiftRight() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class PageUp extends ListActions {
    public static final @NonNls String ID = "scrollUp";

    public PageUp() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftPageUp extends ListActions {
    public static final @NonNls String ID = "scrollUpExtendSelection";

    public ShiftPageUp() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class PageDown extends ListActions {
    public static final @NonNls String ID = "scrollDown";

    public PageDown() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }

  public static final class ShiftPageDown extends ListActions {
    public static final @NonNls String ID = "scrollDownExtendSelection";

    public ShiftPageDown() {
      super(ID);
    }

    @NotNull
    @Override
    public ActionRemoteBehavior getBehavior() {
      return ActionRemoteBehavior.FrontendOnly;
    }
  }
}
