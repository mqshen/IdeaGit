// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.impl.softwrap;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.ArrowPainter;
import com.intellij.openapi.editor.impl.ColorProvider;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * {@link SoftWrapPainter} implementation that draws arrows in soft wrap location.
 * <p/>
 * Primary idea is to use dedicated unicode symbols as soft wrap drawings and this class is introduced only as a part
 * of defensive programming - there is unlikely case that local client environment doesn't have a font that is able to
 * represent target unicode symbol. We draw an arrow manually then (platform-independent approach).
 */
public final class ArrowSoftWrapPainter implements SoftWrapPainter {

  private final HeightProvider myHeightProvider = new HeightProvider();
  private final Editor myEditor;
  private final ArrowPainter myArrowPainter;
  private int myMinWidth = -1;

  public ArrowSoftWrapPainter(Editor editor) {
    myEditor = editor;
    myArrowPainter = new ArrowPainter(ColorProvider.byColor(myEditor.getColorsScheme().getDefaultForeground()), new WidthProvider(), myHeightProvider);
  }

  @Override
  public int paint(@NotNull Graphics g, @NotNull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    myHeightProvider.myHeight = lineHeight / 2;

    int start;
    int end;
    int result;
    switch (drawingType) {
      case BEFORE_SOFT_WRAP_LINE_FEED -> {
        start = x;
        end = myEditor.getScrollingModel().getVisibleArea().width;
        result = end - start;
      }
      case AFTER_SOFT_WRAP -> {
        start = 0;
        end = x;
        result = 0;
      }
      default -> throw new IllegalStateException("Soft wrap arrow painting is not set up for drawing type " + drawingType);
    }
    myArrowPainter.paint(g, y + lineHeight - g.getFontMetrics().getDescent(), start, end);
    return result;
  }

  @Override
  public int getDrawingHorizontalOffset(@NotNull Graphics g, @NotNull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    return switch (drawingType) {
      case BEFORE_SOFT_WRAP_LINE_FEED -> myEditor.getScrollingModel().getVisibleArea().width - x;
      case AFTER_SOFT_WRAP -> 0;
    };
  }

  @Override
  public int getMinDrawingWidth(@NotNull SoftWrapDrawingType drawingType) {
    if (myMinWidth < 0) {
      // We need to reserve a minimal space required for representing arrow before soft wrap-introduced line feed.
      myMinWidth = EditorUtil.charWidth('a', Font.PLAIN, myEditor);
    }
    return myMinWidth;
  }

  @Override
  public boolean canUse() {
    return true;
  }

  @Override
  public void reinit() {
    myMinWidth = -1;
  }

  private static final class HeightProvider implements Computable<Integer> {

    public int myHeight;

    @Override
    public Integer compute() {
      return myHeight;
    }
  }

  private final class WidthProvider implements Computable<Integer> {
    @Override
    public Integer compute() {
      return EditorUtil.getSpaceWidth(Font.PLAIN, myEditor);
    }
  }
}
