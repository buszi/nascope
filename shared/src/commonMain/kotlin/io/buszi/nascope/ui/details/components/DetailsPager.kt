package io.buszi.nascope.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_pager_indicator
import io.buszi.nascope.resources.details_pager_next
import io.buszi.nascope.resources.details_pager_previous
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Previous / next page controls with a `page / pageCount` indicator, for paginated details lists.
 *
 * @param page Zero-based index of the visible page.
 * @param pageCount Total number of pages, at least 1.
 */
@Composable
fun DetailsPager(
    page: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerButton(
            symbol = "‹",
            testTag = TestTags.DetailsPagerPrevious,
            contentDescription = stringResource(Res.string.details_pager_previous),
            enabled = page > 0,
            onClick = onPrevious,
        )
        Text(
            text = stringResource(Res.string.details_pager_indicator, page + 1, pageCount),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.testTag(TestTags.DetailsPagerIndicator),
        )
        PagerButton(
            symbol = "›",
            testTag = TestTags.DetailsPagerNext,
            contentDescription = stringResource(Res.string.details_pager_next),
            enabled = page < pageCount - 1,
            onClick = onNext,
        )
    }
}

@Composable
private fun PagerButton(
    symbol: String,
    testTag: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(AppTheme.dimensions.buttonHeightDense - AppTheme.spacing.spaceXs)
            .background(
                color = if (enabled) AppTheme.colors.inputBackground else AppTheme.colors.canvasBackground,
                shape = AppTheme.shapes.sm,
            )
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = if (enabled) AppTheme.colors.baseBorder else AppTheme.colors.subtleDivider,
                shape = AppTheme.shapes.sm,
            )
            .clip(AppTheme.shapes.sm)
            .optionalModifier(enabled) {
                clickable(
                    onClickLabel = contentDescription,
                    role = Role.Button,
                    onClick = onClick,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = AppTheme.typography.codeBase,
            color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textDisabled,
        )
    }
}

@Preview
@Composable
private fun DetailsPagerPreview() {
    AppTheme {
        DetailsPager(page = 1, pageCount = 3, onPrevious = {}, onNext = {})
    }
}
