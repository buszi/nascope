package io.buszi.nascope.ui.common.components.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.common.util.LocalWindowWidthClass
import io.buszi.nascope.ui.common.util.WindowWidthClass

/**
 * Screen-level layout for focused, single-column flows such as setup and the connection editor.
 *
 * On [WindowWidthClass.Compact] the content fills the width inside the standard margin; on Medium and
 * Expanded windows it becomes a column centred horizontally with a fixed maximum width
 * ([AppTheme.dimensions.setupPanelMaxWidth]). The content is centred vertically and scrolls when it
 * does not fit. With the on-screen keyboard open, a spacer as tall as the part of the keyboard over this panel
 * (`imePadding`, net of the insets the shell already consumed) ends the scrolling content, so every field can be
 * scrolled above the keyboard while the window itself keeps its size.
 *
 * @param modifier Root layout modifier; the layout fills the available space.
 * @param maxWidth Maximum content width on wide windows.
 * @param verticalAlignment Where the content sits when it is shorter than the screen: centred for focused
 * flows, at the top for lists.
 * @param verticalArrangement Spacing between the content children.
 * @param content Column content.
 */
@Composable
fun CenteredPanel(
    modifier: Modifier = Modifier,
    maxWidth: Dp = AppTheme.dimensions.setupPanelMaxWidth,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val margin = when (LocalWindowWidthClass.current) {
        WindowWidthClass.Compact -> AppTheme.spacing.margin
        WindowWidthClass.Medium -> AppTheme.spacing.marginTablet
        WindowWidthClass.Expanded -> AppTheme.spacing.marginDesktop
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = this.maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(margin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.aligned(verticalAlignment),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
                verticalArrangement = verticalArrangement,
                content = content,
            )
            Spacer(Modifier.imePadding())
        }
    }
}

@Preview
@Composable
private fun CenteredPanelPreview() {
    AppTheme {
        CenteredPanel {
            Text(
                text = "alpha-node-01.local",
                style = AppTheme.typography.headlineLg,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = "Content is centred with a fixed maximum width on wide windows.",
                style = AppTheme.typography.bodyMd,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}
