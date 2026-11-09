package io.buszi.nascope

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.containers.ElevatedPanel
import io.buszi.nascope.ui.common.components.containers.TelemetryCard
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.inputs.Keycap
import io.buszi.nascope.ui.common.components.buttons.PrimaryButton
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.inputs.TelemetryTextField
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        var expandedLogs by remember { mutableStateOf(false) }
        var filterQuery by remember { mutableStateOf("") }
        val greeting = remember { Greeting().greet() }

        val egressHistory = remember {
            listOf(
                120f, 135f, 140f, 128f, 150f, 180f, 240f, 320f, 410f, 550f,
                620f, 710f, 780f, 820f, 840f, 830f, 810f, 790f, 825f, 842f,
                815f, 780f, 740f, 705f, 690f, 720f, 750f, 800f, 830f, 845f,
                820f, 795f, 780f, 810f, 835f, 840f, 838f, 841f, 843f, 842.6f,
            )
        }
        val cpuHistory = remember {
            listOf(
                22f, 24f, 21f, 28f, 34f, 32f, 30f, 45f, 62f, 58f,
                54f, 48f, 52f, 68f, 74f, 71f, 65f, 59f, 62f, 64f,
                70f, 78f, 82f, 80f, 75f, 69f, 63f, 58f, 52f, 49f,
                45f, 42f, 40f, 44f, 48f, 52f, 50f, 46f, 43f, 42.8f,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.canvasBackground)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.margin),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            // Header: Section label & Node status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "SYSTEM MONITOR // OBSIDIAN TELEMETRY",
                        style = AppTheme.typography.labelUi,
                        color = AppTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))
                    Text(
                        text = "Node Cluster alpha-01",
                        style = AppTheme.typography.headlineLg,
                        color = AppTheme.colors.textPrimary,
                    )
                }

                StatusChip(
                    text = "ONLINE",
                    status = StatusLevel.Operational,
                    pulsing = true,
                )
            }

            // Metric Cards Grid / Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            ) {
                // Card 1: Egress Metrics (Telemetry Series B)
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "NETWORK EGRESS",
                    metric = "842.6",
                    unit = "MB/s",
                    trend = "+12.4% / 5m",
                    trendColor = AppTheme.colors.operational,
                    accentColor = AppTheme.colors.telemetryB,
                    sparklineData = egressHistory,
                )

                // Card 2: CPU Pipeline (Telemetry Series A)
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "CLUSTER CPU",
                    metric = "42.8",
                    unit = "%",
                    trend = "-1.8% / 5m",
                    trendColor = AppTheme.colors.telemetryA,
                    accentColor = AppTheme.colors.telemetryA,
                    sparklineData = cpuHistory,
                )
            }

            // Resource Allocation Gauges
            TelemetryCard(
                title = "RESOURCE ALLOCATION & THRESHOLDS",
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                    LinearGauge(
                        progress = 0.428f,
                        label = "AGGREGATE CPU USAGE",
                        valueText = "42.8%",
                        normalColor = AppTheme.colors.telemetryA,
                    )
                    LinearGauge(
                        progress = 0.812f,
                        label = "RAM ALLOCATION (SWAP READY)",
                        valueText = "13.0 / 16.0 GB (81.2%)",
                    )
                    LinearGauge(
                        progress = 0.946f,
                        label = "PRIMARY STORAGE POOL (/volume1)",
                        valueText = "94.6% CRITICAL",
                    )
                }
            }

            // Status Badges Overview Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.subSurface, AppTheme.shapes.lg)
                    .border(
                        width = AppTheme.dimensions.hairlineBorder,
                        color = AppTheme.colors.baseBorder,
                        shape = AppTheme.shapes.lg,
                    )
                    .padding(AppTheme.spacing.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SERVICES:",
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textMuted,
                )
                StatusChip(text = "KUBE: HEALTHY", status = StatusLevel.Operational)
                StatusChip(text = "SYNC: LATENCY", status = StatusLevel.Degraded)
                TagChip(text = "QUEUE: 0 DROPS")
            }

            // Controls, Search & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TelemetryTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = "Filter endpoints or CIDR (e.g. 192.168.1.0/24)...",
                    modifier = Modifier.weight(1f),
                    trailingIcon = if (!AppTheme.platform.isMobile) {
                        {
                            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs)) {
                                Keycap(key = "⌘")
                                Keycap(key = "K")
                            }
                        }
                    } else null,
                )

                PrimaryButton(
                    text = if (expandedLogs) "Collapse Telemetry" else "Inspect Telemetry",
                    onClick = { expandedLogs = !expandedLogs },
                )

                SecondaryButton(
                    text = "Export CIDR",
                    onClick = { /* secondary action */ },
                )
            }

            // Animated Log Drawer / Telemetry Details
            AnimatedVisibility(visible = expandedLogs) {
                ElevatedPanel(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "LIVE TELEMETRY STREAM",
                            style = AppTheme.typography.labelUi,
                            color = AppTheme.colors.textSecondary,
                        )
                        if (!AppTheme.platform.isMobile) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Pause:",
                                    style = AppTheme.typography.labelCode,
                                    color = AppTheme.colors.textMuted,
                                )
                                Keycap(key = "Space")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
                    TelemetryLogRow(timestamp = "14:44:02.112", address = "192.168.1.120:9092", latency = "1.42ms", status = "ACK")
                    TelemetryLogRow(timestamp = "14:44:03.491", address = "10.0.4.88:443", latency = "3.11ms", status = "ESTABLISHED")
                    TelemetryLogRow(timestamp = "14:44:04.008", address = "172.16.0.5:8080", latency = "0.85ms", status = "SYNC")

                    Spacer(modifier = Modifier.height(AppTheme.spacing.spaceXs))
                    Text(
                        text = "Target Runtime: $greeting",
                        style = AppTheme.typography.codeBase,
                        color = AppTheme.colors.telemetryA,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    metric: String,
    unit: String,
    trend: String,
    trendColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    sparklineData: List<Float>? = null,
) {
    TelemetryCard(
        modifier = modifier,
        title = title,
        headerAction = {
            StatusDot(color = accentColor)
        },
    ) {
        MetricValue(
            value = metric,
            unit = unit,
            trend = trend,
            trendColor = trendColor,
        )

        if (sparklineData != null) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.spaceSm))
            MiniSparkline(
                data = sparklineData,
                lineColor = accentColor,
            )
        }
    }
}

@Composable
private fun TelemetryLogRow(
    timestamp: String,
    address: String,
    latency: String,
    status: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.subtleDivider,
                shape = AppTheme.shapes.base,
            )
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .padding(horizontal = AppTheme.spacing.spaceSm, vertical = AppTheme.spacing.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = timestamp,
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textMuted,
        )
        Text(
            text = address,
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = latency,
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.telemetryB,
        )
        Text(
            text = status,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.operational,
        )
    }
}
