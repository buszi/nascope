package io.buszi.nascope.ui.dashboard.customize

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.customize_bricks_helper
import io.buszi.nascope.resources.customize_bricks_section
import io.buszi.nascope.resources.customize_drag_handle
import io.buszi.nascope.resources.customize_move_down
import io.buszi.nascope.resources.customize_move_up
import io.buszi.nascope.resources.customize_not_offered
import io.buszi.nascope.resources.customize_reset
import io.buszi.nascope.resources.customize_reset_cancel
import io.buszi.nascope.resources.customize_reset_confirm
import io.buszi.nascope.resources.customize_reset_helper
import io.buszi.nascope.resources.customize_reset_prompt
import io.buszi.nascope.resources.customize_reset_section
import io.buszi.nascope.resources.customize_subtitle
import io.buszi.nascope.resources.customize_title
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.inputs.TelemetrySwitch
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.common.util.minimumTouchTarget
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.framework.title
import io.buszi.nascope.ui.details.components.DetailsLoadingPlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.navigation.components.NavigationGlyph
import io.buszi.nascope.ui.navigation.components.NavigationGlyphType
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardCustomizeScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<DashboardCustomizeViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DashboardCustomizeScreen(
        viewState = viewState,
        onClose = onClose,
        onViewAction = viewModel::onViewAction,
        modifier = modifier,
    )
}

/**
 * The dashboard editor: a row per registered brick in the layout's order, each with a drag handle and a switch, and
 * "Reset to default". It sits in the inspector pane beside the live dashboard (Medium / Expanded) or fills the screen
 * (Compact).
 */
@Composable
fun DashboardCustomizeScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customize = viewState.customize
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    val currentOnViewAction by rememberUpdatedState(onViewAction)
    val dragState = remember {
        BrickDragState { id, offset ->
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            currentOnViewAction(ViewAction.OnDragMove(id, offset))
        }
    }
    // The sections' column below the scroll: row places measured in it do not change while the list scrolls.
    var content by remember { mutableStateOf<LayoutCoordinates?>(null) }

    DragAutoScroll(dragState = dragState, scrollState = scrollState)

    DetailsScaffold(
        title = stringResource(Res.string.customize_title),
        onClose = onClose,
        testTag = TestTags.CustomizeScreen,
        modifier = modifier,
        subtitle = customize?.let { stringResource(Res.string.customize_subtitle, it.enabledCount, it.rows.size) },
        scrollState = scrollState,
    ) {
        if (customize == null) {
            DetailsLoadingPlaceholder()
            return@DetailsScaffold
        }

        DetailsSection(
            title = stringResource(Res.string.customize_bricks_section),
            modifier = Modifier.onPlaced { content = it.parentLayoutCoordinates },
        ) {
            Text(
                text = stringResource(Res.string.customize_bricks_helper),
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textSecondary,
            )
            // Keyed rows move instead of being recreated, so a drag or a focused handle survives its row's moves; the
            // lookahead scope lets every row slide to its new place.
            LookaheadScope {
                Column(modifier = Modifier.padding(top = AppTheme.spacing.spaceSm)) {
                    customize.rows.forEachIndexed { index, row ->
                        key(row.id) {
                            BrickEditorRow(
                                row = row,
                                index = index,
                                canMoveUp = customize.canMoveUp(index),
                                canMoveDown = customize.canMoveDown(index),
                                dragState = dragState,
                                order = { customize.rows.map { it.id } },
                                onViewAction = onViewAction,
                                lookaheadScope = this@LookaheadScope,
                                onPlacedInContent = { coordinates ->
                                    content?.let { parent ->
                                        dragState.onRowPlaced(
                                            id = row.id,
                                            top = parent.localPositionOf(coordinates, Offset.Zero).y,
                                            height = coordinates.size.height,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        ResetSection(
            isCustomized = customize.isCustomized,
            isConfirming = viewState.isConfirmingReset,
            onViewAction = onViewAction,
        )
    }
}

/**
 * Scrolls the editor while the dragged row is held near the top or bottom edge, faster the closer it gets, and moves
 * the row along with the content so it stays under the finger.
 */
@Composable
private fun DragAutoScroll(dragState: BrickDragState, scrollState: ScrollState) {
    val density = LocalDensity.current
    val edge = with(density) { AppTheme.dimensions.minTouchTarget.toPx() }
    val maxStep = with(density) { AppTheme.spacing.spaceSm.toPx() }
    // DetailsScaffold pads its sections' column by the margin, above the place the rows are measured from.
    val contentTop = with(density) { AppTheme.spacing.margin.toPx() }

    LaunchedEffect(dragState.draggedId != null) {
        if (dragState.draggedId == null) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { }
            val span = dragState.draggedSpan() ?: continue
            val top = span.start + contentTop - scrollState.value
            val bottom = span.endInclusive + contentTop - scrollState.value
            val viewport = scrollState.viewportSize.toFloat()
            val step = when {
                top < edge -> -maxStep * ((edge - top) / edge).coerceAtMost(1f)
                bottom > viewport - edge -> maxStep * ((bottom - viewport + edge) / edge).coerceAtMost(1f)
                else -> 0f
            }
            if (step != 0f) {
                val scrolled = scrollState.scrollBy(step)
                if (scrolled != 0f) dragState.dragBy(scrolled)
            }
        }
    }
}

@Composable
private fun BrickEditorRow(
    row: DashboardCustomizeUi.BrickRow,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    dragState: BrickDragState,
    order: () -> List<DashboardBrickId>,
    onViewAction: (ViewAction) -> Unit,
    lookaheadScope: LookaheadScope,
    onPlacedInContent: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDragged = dragState.draggedId == row.id
    val bringIntoView = remember { BringIntoViewRequester() }
    val handleInteraction = remember { MutableInteractionSource() }
    val isHandleFocused by handleInteraction.collectIsFocusedAsState()

    LaunchedEffect(index) {
        // A keyboard move keeps the focus on this row's handle; keep the row on screen for the next press.
        if (isHandleFocused) bringIntoView.bringIntoView()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Before the animation: these see where the row goes, not where it is drawn while it slides there.
            .onPlaced(onPlacedInContent)
            .bringIntoViewRequester(bringIntoView)
            // The dragged row follows the finger, not an animation of its place.
            .optionalModifier(!isDragged) { animateBounds(lookaheadScope) }
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer { translationY = if (isDragged) dragState.offset else 0f }
            .optionalModifier(isDragged) {
                background(AppTheme.colors.elevatedOverlay, AppTheme.shapes.base)
                    .border(AppTheme.dimensions.hairlineBorder, AppTheme.colors.focusHighlight, AppTheme.shapes.base)
            }
            .testTag(TestTags.customizeRow(row.key))
            // Compact already grows the handle and the switch to the 48 dp touch target, which spaces the rows.
            .padding(
                vertical = if (AppTheme.windowWidthClass == WindowWidthClass.Compact) {
                    AppTheme.spacing.spaceXs
                } else {
                    AppTheme.spacing.spaceSm
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        val title = row.id.title()
        DragHandle(
            row = row,
            title = title,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            dragState = dragState,
            order = order,
            interactionSource = handleInteraction,
            onViewAction = onViewAction,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            // A brick that is off keeps its place, dimmed, so turning it back on restores where it was.
            Text(
                text = title,
                style = AppTheme.typography.labelUi,
                color = if (row.isEnabled) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!row.isOffered) {
                Text(
                    text = stringResource(Res.string.customize_not_offered),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TelemetrySwitch(
            checked = row.isEnabled,
            onCheckedChange = { enabled -> onViewAction(ViewAction.OnToggleBrick(row.id, enabled)) },
            modifier = Modifier
                .semantics { contentDescription = title }
                .testTag(TestTags.customizeSwitch(row.key)),
        )
    }
}

/**
 * The handle a row is reordered by: dragged with touch or a mouse, Alt+Up / Alt+Down when focused, and "Move up" /
 * "Move down" actions for screen readers. The drag writes once, on drop; the keys and actions write every step.
 */
@Composable
private fun DragHandle(
    row: DashboardCustomizeUi.BrickRow,
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    dragState: BrickDragState,
    order: () -> List<DashboardBrickId>,
    interactionSource: MutableInteractionSource,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val handleLabel = stringResource(Res.string.customize_drag_handle, title)
    val moveUpLabel = stringResource(Res.string.customize_move_up, title)
    val moveDownLabel = stringResource(Res.string.customize_move_down, title)
    val currentOrder by rememberUpdatedState(order)
    val currentOnViewAction by rememberUpdatedState(onViewAction)
    val draggable = rememberDraggableState { delta -> dragState.dragBy(delta) }

    // Leaving the screen mid-drag (e.g. the pane closes) gets no stop from the gesture: give the stored order back.
    DisposableEffect(row.id) {
        onDispose {
            if (dragState.draggedId == row.id) {
                dragState.stop()
                currentOnViewAction(ViewAction.OnDragCancel)
            }
        }
    }

    Box(
        modifier = modifier
            .minimumTouchTarget()
            .size(AppTheme.dimensions.buttonHeightDense)
            .clip(AppTheme.shapes.base)
            // Only the dots at rest; the ring shows where the keyboard is.
            .optionalModifier(isFocused) {
                border(AppTheme.dimensions.focusRing, AppTheme.colors.telemetryA, AppTheme.shapes.base)
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .draggable(
                state = draggable,
                orientation = Orientation.Vertical,
                // Pressing the handle is meant to drag; on touch the row itself still scrolls the list.
                startDragImmediately = true,
                onDragStarted = {
                    dragState.start(row.id, currentOrder())
                    currentOnViewAction(ViewAction.OnDragStart)
                },
                onDragStopped = {
                    if (dragState.draggedId == row.id) {
                        currentOnViewAction(ViewAction.OnDrop)
                        // Settle into the place from wherever the finger let go.
                        animate(initialValue = dragState.offset, targetValue = 0f) { value, _ -> dragState.settleTo(value) }
                        dragState.stop()
                    }
                },
            )
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onKeyEvent false
                when {
                    event.key == Key.DirectionUp && canMoveUp -> onViewAction(ViewAction.OnMoveUp(row.id))
                    event.key == Key.DirectionDown && canMoveDown -> onViewAction(ViewAction.OnMoveDown(row.id))
                    else -> return@onKeyEvent false
                }
                true
            }
            .focusable(interactionSource = interactionSource)
            .semantics {
                contentDescription = handleLabel
                customActions = listOfNotNull(
                    CustomAccessibilityAction(moveUpLabel) { onViewAction(ViewAction.OnMoveUp(row.id)); true }
                        .takeIf { canMoveUp },
                    CustomAccessibilityAction(moveDownLabel) { onViewAction(ViewAction.OnMoveDown(row.id)); true }
                        .takeIf { canMoveDown },
                )
            }
            .testTag(TestTags.customizeHandle(row.key)),
        contentAlignment = Alignment.Center,
    ) {
        NavigationGlyph(
            type = NavigationGlyphType.DragHandle,
            tint = if (dragState.draggedId == row.id) AppTheme.colors.telemetryA else AppTheme.colors.textSecondary,
            iconSize = AppTheme.spacing.spaceLg,
        )
    }
}

@Composable
private fun ResetSection(
    isCustomized: Boolean,
    isConfirming: Boolean,
    onViewAction: (ViewAction) -> Unit,
) {
    DetailsSection(title = stringResource(Res.string.customize_reset_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            if (isConfirming) {
                Text(
                    text = stringResource(Res.string.customize_reset_prompt),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                    SecondaryButton(
                        text = stringResource(Res.string.customize_reset_cancel),
                        onClick = { onViewAction(ViewAction.OnCancelReset) },
                        size = ButtonSize.Regular,
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.CustomizeResetCancel),
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.customize_reset_confirm),
                        onClick = { onViewAction(ViewAction.OnConfirmReset) },
                        size = ButtonSize.Regular,
                        modifier = Modifier
                            .weight(1f)
                            .minimumTouchTarget()
                            .testTag(TestTags.CustomizeResetConfirm),
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.customize_reset_helper),
                    style = AppTheme.typography.bodySm,
                    color = AppTheme.colors.textSecondary,
                )
                SecondaryButton(
                    text = stringResource(Res.string.customize_reset),
                    onClick = { onViewAction(ViewAction.OnRequestReset) },
                    size = ButtonSize.Regular,
                    enabled = isCustomized,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                        .testTag(TestTags.CustomizeReset),
                )
            }
        }
    }
}

@Preview
@Composable
private fun DashboardCustomizeScreenPreview(
    @PreviewParameter(DashboardCustomizePreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        DashboardCustomizeScreen(viewState = viewState, onClose = {}, onViewAction = {})
    }
}
