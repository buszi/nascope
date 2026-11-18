package io.buszi.nascope.snapshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.buszi.nascope.App
import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.domain.dashboard.SetDashboardLayoutUseCase
import io.buszi.nascope.snapshot.SnapshotScript.Command
import org.koin.core.context.GlobalContext
import java.io.File
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.minutes

/**
 * Runs [SnapshotScript] commands against the real [App] in an offscreen Compose scene.
 *
 * Frames are driven by hand (`autoAdvance` off) while real time passes, because the app's data arrives on real
 * threads (polling, DataStore) and its pulsing chips are infinite animations that never let the scene go idle.
 */
@OptIn(ExperimentalTestApi::class)
internal class SnapshotRunner(
    private val out: File,
    /** Pixels per dp in the written images. */
    private val scale: Float,
) {

    /** @return `false` when a command failed; the error is printed with its script line. */
    fun run(commands: List<Command>): Boolean {
        val sizes = commands.filterIsInstance<SnapshotScript.Size>()
        val sceneWidth = (sizes.map { it.width } + DefaultSize.width.value.toInt()).max()
        val sceneHeight = (sizes.map { it.height } + DefaultSize.height.value.toInt()).max()

        var failed: Command? = null
        try {
            runSkikoComposeUiTest(
                size = Size(sceneWidth * scale, sceneHeight * scale),
                density = Density(scale),
                testTimeout = 30.minutes,
            ) {
                mainClock.autoAdvance = false
                val scene = SceneState()
                setContent {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(scene.size)
                                .testTag(RootTag),
                        ) {
                            if (scene.started) App()
                        }
                    }
                }
                val watchdog = CommandWatchdog()
                for (command in commands) {
                    failed = command
                    println("snapshot: line ${command.line}: ${command.describe()}")
                    watchdog.arm("line ${command.line} (${command.describe()})", (command as? SnapshotScript.Wait)?.millis ?: 0L)
                    execute(command, scene)
                    watchdog.disarm()
                }
                failed = null
            }
        } catch (e: Throwable) {
            val where = failed?.let { "line ${it.line} (${it.describe()})" } ?: "the app"
            System.err.println("snapshot: FAILED at $where: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            return false
        }
        return true
    }

    private class SceneState {
        var started by mutableStateOf(false)
        var size by mutableStateOf(DefaultSize)
    }

    private suspend fun SkikoComposeUiTest.execute(command: Command, scene: SceneState) {
        when (command) {
            is SnapshotScript.Demo -> GlobalContext.get().get<SetDemoModeUseCase>().invoke(command.enabled)
            is SnapshotScript.Layout -> GlobalContext.get().get<SetDashboardLayoutUseCase>().invoke(command.layout)
            is SnapshotScript.Start -> start(scene)
            is SnapshotScript.Size -> {
                scene.size = DpSize(command.width.dp, command.height.dp)
                pump(SettleMillis)
            }
            is SnapshotScript.Wait -> {
                start(scene)
                pump(command.millis)
            }
            is SnapshotScript.Shot -> {
                start(scene)
                val image = runWithoutImplicitWait { captureToImage(onNodeWithTag(RootTag).fetchSemanticsNode()) }
                val file = File(out, "${command.name}.png")
                ImageIO.write(image.toAwtImage(), "png", file)
                println("snapshot:   wrote ${file.path}")
            }
            is SnapshotScript.Tree -> {
                start(scene)
                val tree = runWithoutImplicitWait { onRoot(useUnmergedTree = true).printToString(maxDepth = Int.MAX_VALUE) }
                val file = File(out, "${command.name}.txt")
                file.writeText(tree)
                println("snapshot:   wrote ${file.path}")
            }
            is SnapshotScript.Click -> interact(scene) { single(Target.Tag(command.tag)).performClick() }
            is SnapshotScript.ClickText -> interact(scene) { single(Target.Text(command.text)).performClick() }
            is SnapshotScript.ScrollTo -> {
                start(scene)
                scrollTo(containerTag = command.containerTag, tag = command.tag)
            }
            is SnapshotScript.Drag -> interact(scene) {
                val steps = DragSteps
                val step = Offset(command.dx * scale / steps, command.dy * scale / steps)
                single(Target.Tag(command.tag)).performMouseInput {
                    moveTo(center)
                    press()
                    repeat(steps) { moveBy(step) }
                    release()
                }
            }
            is SnapshotScript.Key -> interact(scene) {
                onRoot().performKeyInput {
                    if (command.alt) withKeyDown(Key.AltLeft) { pressKey(command.key()) } else pressKey(command.key())
                }
            }
        }
    }

    private sealed interface Target {
        data class Tag(val tag: String) : Target
        data class Text(val text: String) : Target
    }

    private fun SkikoComposeUiTest.all(target: Target): SemanticsNodeInteractionCollection = when (target) {
        is Target.Tag -> onAllNodesWithTag(target.tag, useUnmergedTree = true)
        is Target.Text -> onAllNodesWithText(target.text, substring = true)
    }

    /**
     * The one node matching [target]. Checked here with a short message: the test library's own "expected exactly one
     * node" error prints the whole semantics tree for every candidate, which takes minutes on the dashboard.
     */
    private fun SkikoComposeUiTest.single(target: Target): SemanticsNodeInteraction {
        val count = runWithoutImplicitWait { all(target).fetchSemanticsNodes(atLeastOneRootRequired = false).size }
        check(count == 1) {
            if (count == 0) "nothing matches $target on screen (below the fold? `scrollto` it first; `tree` lists what is there)"
            else "$count nodes match $target"
        }
        return all(target)[0]
    }

    /**
     * Scrolls [containerTag] by most of its height until a node tagged [tag] is composed, then brings it into view.
     * Goes down first and turns back up once the container stops moving (the end of the list).
     */
    private fun SkikoComposeUiTest.scrollTo(containerTag: String, tag: String) {
        val container = single(Target.Tag(containerTag))
        var direction = 1f
        repeat(MaxScrolls) {
            val found = runWithoutImplicitWait {
                onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).size
            }
            if (found > 0) {
                // Line its top up with the container's (minus a margin). The library's performScrollTo can loop
                // forever on the dashboard grid, so this is done by hand with one scroll.
                val target = runWithoutImplicitWait { single(Target.Tag(tag)).fetchSemanticsNode() }
                val view = runWithoutImplicitWait { container.fetchSemanticsNode() }
                val delta = target.boundsInRoot.top - view.boundsInRoot.top - IntoViewMarginDp * scale
                runWithoutImplicitWait { container.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) } }
                pump(SettleMillis)
                return
            }
            val node = runWithoutImplicitWait { container.fetchSemanticsNode() }
            check(SemanticsActions.ScrollBy in node.config) { "'$containerTag' cannot scroll" }
            val before = node.verticalScrollPosition()
            runWithoutImplicitWait {
                container.performSemanticsAction(SemanticsActions.ScrollBy) {
                    it(0f, direction * node.size.height * ScrollFraction)
                }
            }
            pump(SettleMillis)
            if (runWithoutImplicitWait { container.fetchSemanticsNode() }.verticalScrollPosition() == before) {
                if (direction < 0f) error("no node tagged '$tag' anywhere in '$containerTag'")
                direction = -1f
            }
        }
        error("no node tagged '$tag' in '$containerTag' after $MaxScrolls scrolls")
    }

    private fun SemanticsNode.verticalScrollPosition(): Float? =
        config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke()

    private fun SkikoComposeUiTest.start(scene: SceneState) {
        if (scene.started) return
        scene.started = true
        pump(SettleMillis)
    }

    private fun SkikoComposeUiTest.interact(scene: SceneState, action: SkikoComposeUiTest.() -> Unit) {
        start(scene)
        runWithoutImplicitWait { action() }
        pump(SettleMillis)
    }

    /** Draws frames while [millis] of real time pass, so work on real threads (polling, storage) lands in between. */
    private fun SkikoComposeUiTest.pump(millis: Long) {
        val end = System.nanoTime() + millis * 1_000_000
        do {
            mainClock.advanceTimeBy(FrameMillis)
            Thread.sleep(FrameMillis)
        } while (System.nanoTime() < end)
    }

    private fun SnapshotScript.Key.key(): Key = when (name) {
        "tab" -> Key.Tab
        "enter" -> Key.Enter
        "space" -> Key.Spacebar
        "escape" -> Key.Escape
        "up" -> Key.DirectionUp
        "down" -> Key.DirectionDown
        "left" -> Key.DirectionLeft
        "right" -> Key.DirectionRight
        else -> error("unknown key $name")
    }

    private fun Command.describe(): String = toString().substringBefore('(').lowercase() +
        toString().substringAfter('(').removeSuffix(")").replace(Regex("line=\\d+,? ?"), "").let { if (it.isBlank()) "" else " $it" }

    private companion object {
        const val RootTag = "snapshot_root"
        const val FrameMillis = 16L
        const val SettleMillis = 250L
        const val DragSteps = 12
        const val MaxScrolls = 30
        const val ScrollFraction = 0.8f
        const val IntoViewMarginDp = 24f
        val DefaultSize = DpSize(1440.dp, 900.dp)
    }
}
