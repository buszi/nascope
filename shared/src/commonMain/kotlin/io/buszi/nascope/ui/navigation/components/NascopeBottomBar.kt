package io.buszi.nascope.ui.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.navigation.TopLevelDestination
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom navigation bar for Mobile / Compact form factors ([AppTheme.dimensions.bottomBarHeight]).
 *
 * Displays horizontal tabs for all [TopLevelDestination] entries on Layer 1 `subSurface`
 * separated from the workspace above by a hairline `baseBorder`.
 *
 * @param activeDestination Currently selected [TopLevelDestination].
 * @param onSelectDestination Callback invoked when a bottom tab is clicked.
 * @param modifier Root layout modifier applied to the bottom bar container.
 */
@Composable
fun NascopeBottomBar(
    activeDestination: TopLevelDestination,
    onSelectDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.subSurface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.baseBorder),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.bottomBarHeight)
                .padding(horizontal = AppTheme.spacing.spaceSm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = destination == activeDestination,
                    onClick = { onSelectDestination(destination) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.telemetryA else Color.Transparent,
        label = "BottomBarIndicator",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.telemetryA else AppTheme.colors.textMuted,
        label = "BottomBarIcon",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
        label = "BottomBarLabel",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag(TestTags.navigation(destination.name))
            .semantics { this.selected = selected }
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .width(AppTheme.spacing.space2xl)
                .height(AppTheme.dimensions.navIndicatorThickness)
                .background(indicatorColor, AppTheme.shapes.full),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            NavigationGlyph(
                type = destination.toGlyphType(),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceXs))
            Text(
                text = stringResource(destination.labelRes),
                style = AppTheme.typography.labelUi,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun NascopeBottomBarPreview() {
    AppTheme {
        NascopeBottomBar(
            activeDestination = TopLevelDestination.Dashboard,
            onSelectDestination = {},
        )
    }
}
