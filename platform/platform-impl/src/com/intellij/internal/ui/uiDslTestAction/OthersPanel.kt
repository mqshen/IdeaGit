// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.internal.ui.uiDslTestAction

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.*
import org.jetbrains.annotations.ApiStatus
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

@Suppress("DialogTitleCapitalization")
@ApiStatus.Internal
internal class OthersPanel {

  val panel: DialogPanel = panel {
    group("DslLabel text update") {
      lateinit var dslText: JEditorPane

      row {
        dslText = text("Initial text with a <a href='link'>link</a>", action = object: HyperlinkEventAction {
          override fun hyperlinkActivated(e: HyperlinkEvent) {
            Messages.showMessageDialog("Link '${e.description}' is clicked", "Message", null)
          }
        }).component
      }
      row {
        val textField = textField()
          .text("New text with <a href='another link'>another link</a><br>Second line")
          .columns(COLUMNS_LARGE)
          .component
        button("Update") {
          dslText.text = textField.text
        }
      }
    }

    group("Size groups") {
      row {
        button("Button", {}).widthGroup("group1")
        button("A very long button", {}).widthGroup("group1")
      }.rowComment("Buttons with the same widthGroup")
    }
  }
}