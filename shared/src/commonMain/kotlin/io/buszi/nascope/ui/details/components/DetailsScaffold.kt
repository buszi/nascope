package io.buszi.nascope.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.screen_details_close
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.WindowWidthClass
import io.buszi.nascope.ui.dashboard.widgets.WidgetSkeleton
import io.buszi.nascope.ui.navigation.components.NavigationGlyph
import io.buszi.nascope.ui.navigation.components.NavigationGlyphType
import org.jetbrains.compose.resources.stringResource

/**
 * Shared frame of every brick details screen.
 *
 * Renders a header (title, optional subtitle, status tag and close action) over a hairline divider,
 * followed by a vertically scrollable column of [DetailsSection]s. The same frame is used in the
 * right-docked inspector pane (Medium/Expanded) and as a full-screen destination (Compact).
 * On Compact the top bar already provides back navigation, so the close action is omitted.
 *
 * @param title Uppercase label identifying the inspected subsystem.
 * @param onClose Closes the details.
 * @param modifier Root layout modifier supplied by the hosting pane / screen.
 * @param subtitle Optional one-line summary rendered below [title].
 * @param statusTag Optional status badge rendered next to the close action.
 * @param content Sections of the details screen.
 */
@Composable
fun DetailsScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    statusTag: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.spacing.margin,
                    vertical = AppTheme.spacing.spaceMd,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTheme.typography.bodySm,
                        color = AppTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            statusTag?.invoke()

            if (AppTheme.windowWidthClass != WindowWidthClass.Compact) {
                CloseButton(onClick = onClose)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.baseBorder),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.margin),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            content = content,
        )
    }
}

/**
 * Titled card grouping one aspect of a brick's details inside a [DetailsScaffold].
 *
 * @param title Uppercase section label.
 * @param modifier Root layout modifier.
 * @param headerAction Optional trailing header slot, e.g. a [TagChip].
 * @param content Section body.
 */
@Composable
fun DetailsSection(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    TelemetryCard(
        modifier = modifier.fillMaxWidth(),
        title = title,
        headerAction = headerAction,
        contentPadding = PaddingValues(AppTheme.spacing.spaceMd),
        content = content,
    )
}

/**
 * Left-aligned label with a right-aligned value, used for property readouts inside sections.
 */
@Composable
fun DetailsKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Skeleton shown inside a [DetailsScaffold] while the brick's telemetry is still loading.
 */
@Composable
fun DetailsLoadingPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        repeat(3) {
            WidgetSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.brickHeightUnit / 2),
            )
        }
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AppTheme.dimensions.buttonHeightDense - AppTheme.spacing.spaceXs)
            .clip(AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .clickable(
                onClickLabel = stringResource(Res.string.screen_details_close),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        NavigationGlyph(
            type = NavigationGlyphType.Close,
            tint = AppTheme.colors.textSecondary,
            iconSize = AppTheme.spacing.spaceLg - AppTheme.spacing.space2xs,
        )
    }
}

@Preview
@Composable
private fun DetailsScaffoldPreview() {
    AppTheme {
        DetailsScaffold(
            title = "SAMPLE // DETAILS",
            subtitle = "Preview subtitle",
            onClose = {},
            statusTag = { TagChip(text = "TAG") },
        ) {
            DetailsSection(title = "SECTION") {
                DetailsKeyValueRow(label = "KEY", value = "value")
            }
        }
    }
}
