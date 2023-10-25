// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.actionSystem.impl

import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.ui.UISettings
import com.intellij.internal.statistic.collectors.fus.actions.persistence.MainMenuCollector
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionMenu.Companion.isAligned
import com.intellij.openapi.actionSystem.impl.ActionMenu.Companion.isAlignedInGroup
import com.intellij.openapi.actionSystem.impl.ActionMenu.Companion.isShowNoIcons
import com.intellij.openapi.actionSystem.impl.ActionMenu.Companion.shouldConvertIconToDarkVariant
import com.intellij.openapi.actionSystem.impl.ActionMenu.Companion.showDescriptionInStatusBar
import com.intellij.openapi.actionSystem.impl.actionholder.createActionRef
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.util.IconLoader.getDarkIcon
import com.intellij.openapi.util.IconLoader.getDisabledIcon
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.JBCheckBoxMenuItem
import com.intellij.ui.icons.getMenuBarIcon
import com.intellij.ui.mac.screenmenu.Menu
import com.intellij.ui.mac.screenmenu.MenuItem
import com.intellij.ui.plaf.beg.BegMenuItemUI
import com.intellij.ui.popup.KeepingPopupOpenAction
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.LafIconLookup.getDisabledIcon
import com.intellij.util.ui.LafIconLookup.getIcon
import com.intellij.util.ui.LafIconLookup.getSelectedIcon
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.JMenuItem
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

internal fun isEnterKeyStroke(keyStroke: KeyStroke): Boolean {
  return keyStroke.keyCode == KeyEvent.VK_ENTER && keyStroke.modifiers == 0
}

class ActionMenuItem internal constructor(action: AnAction,
                                          @JvmField val place: String,
                                          private val context: DataContext,
                                          private val enableMnemonics: Boolean,
                                          private val insideCheckedGroup: Boolean,
                                          private val useDarkIcons: Boolean) : JBCheckBoxMenuItem() {

  private val actionRef = createActionRef(action)
  val isToggleable: Boolean = action is Toggleable

  @JvmField
  internal val screenMenuItemPeer: MenuItem?

  @NlsSafe
  private var description: String? = null
  private var isToggled = false
  var isKeepMenuOpen: Boolean = false
    private set

  init {
    addActionListener(ActionListener { e -> performAction(e.modifiers) })
    setBorderPainted(false)
    if (Menu.isJbScreenMenuEnabled() && ActionPlaces.MAIN_MENU == this.place) {
      screenMenuItemPeer = MenuItem()
      screenMenuItemPeer.setActionDelegate(Runnable {

        // Called on AppKit when user activates menu item
        if (isToggleable) {
          isToggled = !isToggled
          screenMenuItemPeer.setState(isToggled)
        }
        SwingUtilities.invokeLater(Runnable {
          if (actionRef.getAction().isEnabledInModalContext || this.context.getData(PlatformCoreDataKeys.IS_MODAL_CONTEXT) != true) {
            (TransactionGuard.getInstance() as TransactionGuardImpl).performUserActivity(
              Runnable { performAction(0) })
          }
        })
      })
    }
    else {
      screenMenuItemPeer = null
    }
    updateUI()
    updateAccelerator()
  }

  val anAction: AnAction
    get() = actionRef.getAction()

  public override fun fireActionPerformed(event: ActionEvent) {
    val app = ApplicationManager.getApplication()
    if (!app.isDisposed() && ActionPlaces.MAIN_MENU == place) {
      MainMenuCollector.getInstance().record(actionRef.getAction())
    }
    (TransactionGuard.getInstance() as TransactionGuardImpl).performUserActivity(
      Runnable { super.fireActionPerformed(event) })
  }

  private fun updateAccelerator() {
    val action = actionRef.getAction()
    val id = ActionManager.getInstance().getId(action)
    if (id != null) {
      setAcceleratorFromShortcuts(KeymapUtil.getActiveKeymapShortcuts(id).getShortcuts())
    }
    else {
      val shortcutSet = action.shortcutSet
      setAcceleratorFromShortcuts(shortcutSet.getShortcuts())
    }
  }

  fun updateFromPresentation(presentation: Presentation) {
    // all items must be visible at this point
    //setVisible(presentation.isVisible());
    setEnabled(presentation.isEnabled)
    val text = ActionPresentationDecorator.decorateTextIfNeeded(actionRef.getAction() ,presentation.getText(enableMnemonics))
    setText(text)
    mnemonic = presentation.getMnemonic()
    displayedMnemonicIndex = presentation.getDisplayedMnemonicIndex()
    updateIcon(presentation)
    description = presentation.description
    isKeepMenuOpen = isKeepMenuOpen || presentation.isMultiChoice || actionRef.getAction() is KeepingPopupOpenAction
    if (screenMenuItemPeer != null) {
      screenMenuItemPeer.setLabel(text, accelerator)
      screenMenuItemPeer.setEnabled(isEnabled)
    }
    val shortcutSuffix = presentation.getClientProperty(Presentation.PROP_KEYBOARD_SHORTCUT_SUFFIX)
    val shortcut = defaultFirstShortcutText
    firstShortcutTextFromPresentation = if (!shortcut.isNullOrEmpty() && !shortcutSuffix.isNullOrEmpty()) {
      shortcut + shortcutSuffix
    }
    else {
      null
    }
  }

  @Throws(IllegalArgumentException::class)
  override fun setDisplayedMnemonicIndex(index: Int) {
    super.setDisplayedMnemonicIndex(if (enableMnemonics) index else -1)
  }

  override fun setMnemonic(mnemonic: Int) {
    super.setMnemonic(if (enableMnemonics) mnemonic else 0)
  }

  private fun setAcceleratorFromShortcuts(shortcuts: Array<Shortcut>) {
    for (shortcut in shortcuts) {
      if (shortcut is KeyboardShortcut) {
        val firstKeyStroke = shortcut.firstKeyStroke
        // If the action has `Enter` shortcut, do not add it. Otherwise, user won't be able to choose any ActionMenuItem other than that
        if (!isEnterKeyStroke(firstKeyStroke)) {
          setAccelerator(firstKeyStroke)
          screenMenuItemPeer?.setLabel(text, firstKeyStroke)
          if (KeymapUtil.isSimplifiedMacShortcuts()) {
            val shortcutText = KeymapUtil.getPreferredShortcutText(shortcuts)
            putClientProperty("accelerator.text", shortcutText)
            screenMenuItemPeer?.setAcceleratorText(shortcutText)
          }
        }
        break
      }
    }
  }

  override fun updateUI() {
    setUI(BegMenuItemUI.createUI(this))
  }

  /**
   * Updates long description of action at the status bar.
   */
  override fun menuSelectionChanged(isIncluded: Boolean) {
    super.menuSelectionChanged(isIncluded)
    showDescriptionInStatusBar(isIncluded = isIncluded, component = this, description = description)
  }

  private var firstShortcutTextFromPresentation: @NlsSafe String? = null

  private val defaultFirstShortcutText: @NlsSafe String
    get() = KeymapUtil.getFirstKeyboardShortcutText(actionRef.getAction())

  val firstShortcutText: @NlsSafe String
    get() = firstShortcutTextFromPresentation ?: defaultFirstShortcutText

  private fun updateIcon(presentation: Presentation) {
    isToggled = isToggleable && Toggleable.isSelected(presentation)
    if (isToggleable && (presentation.icon == null || insideCheckedGroup || !UISettings.getInstance().showIconsInMenus)) {
      if (ActionPlaces.MAIN_MENU == place && SystemInfo.isMacSystemMenu) {
        state = isToggled
        screenMenuItemPeer?.setState(isToggled)
        //setIcon(wrapNullIcon(icon)!!)
      }
      else if (isToggled) {
        setToggledIcon()
      }
      else {
        setIcon(EmptyIcon.ICON_16)
        setSelectedIcon(EmptyIcon.ICON_16)
        setDisabledIcon(EmptyIcon.ICON_16)
      }
    }
    else if (UISettings.getInstance().showIconsInMenus) {
      var icon = presentation.icon
      if (isToggleable && isToggled) {
        icon = PoppedIcon(icon, 16, 16)
      }
      var disabled = presentation.disabledIcon
      if (disabled == null) {
        disabled = if (icon == null) null else getDisabledIcon(icon)
      }
      var selected = presentation.selectedIcon
      if (selected == null) {
        selected = icon
      }
      setIcon(wrapNullIcon(if (presentation.isEnabled) icon else disabled))
      setSelectedIcon(wrapNullIcon(selected))
      setDisabledIcon(wrapNullIcon(disabled))
    }
  }

  private fun wrapNullIcon(icon: Icon?): Icon? {
    val isMainMenu = ActionPlaces.MAIN_MENU == place
    if (isMainMenu && isShowNoIcons(actionRef.getAction())) {
      return null
    }
    if (!isAligned || !isAlignedInGroup) {
      return icon
    }
    return if (icon == null && SystemInfo.isMacSystemMenu && ActionPlaces.MAIN_MENU == place) EMPTY_MENU_ACTION_ICON else icon
  }

  override fun setIcon(icon: Icon?) {
    var effectiveIcon: Icon? = icon
    if (effectiveIcon != null) {
      if (SystemInfo.isMacSystemMenu && ActionPlaces.MAIN_MENU == place) {
        // JDK can't correctly paint our HiDPI icons at the system menu bar
        effectiveIcon = getMenuBarIcon(effectiveIcon, useDarkIcons)
      }
      else if (shouldConvertIconToDarkVariant()) {
        effectiveIcon = getDarkIcon(effectiveIcon, true)
      }
    }
    super.setIcon(effectiveIcon)
    screenMenuItemPeer?.setIcon(effectiveIcon)
  }

  override fun isSelected(): Boolean = isToggled

  private fun performAction(modifiers: Int) {
    val action = actionRef.getAction()
    val id = ActionManager.getInstance().getId(action)
    if (id != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("context.menu.click.stats.${id.replace(' ', '.')}")
    }
    IdeFocusManager.findInstanceByContext(context).runOnOwnContext(context, Runnable {
      val currentEvent = IdeEventQueue.getInstance().trueCurrentEvent
      val event = AnActionEvent(
        /* inputEvent = */ if (currentEvent is InputEvent) currentEvent else null,
        /* dataContext = */ context,
        /* place = */ place,
        /* presentation = */ action.getTemplatePresentation().clone(),
        /* actionManager = */ ActionManager.getInstance(),
        /* modifiers = */ modifiers,
        /* isContextMenuAction = */ true,
        /* isActionToolbar = */ false)
      if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
        ActionUtil.performActionDumbAwareWithCallbacks(action, event)
      }
    })
  }
}

internal fun JMenuItem.setToggledIcon() {
  var checkmark = getIcon("checkmark")
  var selectedCheckmark = getSelectedIcon("checkmark")
  var disabledCheckmark = getDisabledIcon("checkmark")
  if (shouldConvertIconToDarkVariant()) {
    checkmark = getDarkIcon(checkmark, true)
    selectedCheckmark = getDarkIcon(selectedCheckmark, true)
    disabledCheckmark = getDarkIcon(disabledCheckmark, true)
  }
  setIcon(checkmark)
  setSelectedIcon(selectedCheckmark)
  setDisabledIcon(disabledCheckmark)
}
