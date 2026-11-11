package io.buszi.nascope.ui.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.navigation.TopLevelDestination
import org.jetbrains.compose.resources.stringResource

/**
 * 56dp compact icon sidebar rail for Tablet / Medium and Desktop / Expanded form factors
 * ([AppTheme.dimensions.sidebarTabletWidth]).
 *
 * Renders the top brand mark, centered destination icon toggles ([TopLevelDestination]),
 * and a bottom operational health indicator dot on Layer 1 `subSurface`.
 *
 * @param activeDestination Currently selected [TopLevelDestination].
 * @param onSelectDestination Callback invoked when a rail icon is clicked.
 * @param modifier Root layout modifier applied to the rail container.
 */
@Composable
fun NascopeSidebarRail(
    activeDestination: TopLevelDestination,
    onSelectDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(AppTheme.dimensions.sidebarTabletWidth)
            .fillMaxHeight()
            .background(AppTheme.colors.subSurface),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = AppTheme.spacing.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
            ) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.dimensions.buttonHeightRegular)
                        .background(AppTheme.colors.elevatedOverlay, AppTheme.shapes.md)
                        .border(
                            width = AppTheme.dimensions.hairlineBorder,
                            color = AppTheme.colors.elevatedBorder,
                            shape = AppTheme.shapes.md,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    NavigationGlyph(
                        type = NavigationGlyphType.BrandMark,
                        tint = AppTheme.colors.telemetryA,
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.spaceXs))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        RailNavigationItem(
                            destination = destination,
                            selected = destination == activeDestination,
                            onClick = { onSelectDestination(destination) },
                        )
                    }
                }
            }

            StatusDot(
                color = AppTheme.colors.operational,
                haloColor = AppTheme.colors.operationalHalo,
                pulsing = true,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.baseBorder),
        )
    }
}

@Composable
private fun RailNavigationItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(destination.labelRes)
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.elevatedOverlay else Color.Transparent,
        label = "RailItemBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.telemetryA else Color.Transparent,
        label = "RailItemBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.telemetryA else AppTheme.colors.textMuted,
        label = "RailItemIcon",
    )

    Box(
        modifier = Modifier
            .size(AppTheme.dimensions.buttonHeightRegular)
            .clip(AppTheme.shapes.md)
            .background(backgroundColor, AppTheme.shapes.md)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = borderColor,
                shape = AppTheme.shapes.md,
            )
            .testTag(TestTags.navigation(destination.name))
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        NavigationGlyph(
            type = destination.toGlyphType(),
            tint = iconTint,
        )
    }
}

@Preview
@Composable
private fun NascopeSidebarRailPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .height(AppTheme.dimensions.brickHeightUnit * 2)
                .background(AppTheme.colors.canvasBackground),
        ) {
            NascopeSidebarRail(
                activeDestination = TopLevelDestination.Dashboard,
                onSelectDestination = {},
            )
        }
    }
}
