package io.buszi.nascope.ui.common.testing

import androidx.compose.ui.Modifier

/**
 * Exposes `testTag`s of this node and its descendants as accessibility resource ids, so UI automation
 * (UiAutomator / Appium) can locate them with `By.id`. Apply once at the root of every composition
 * (and on popups / dialogs, which are separate compositions). No-op where there is no such concept.
 */
expect fun Modifier.testTagsAsResourceId(): Modifier
