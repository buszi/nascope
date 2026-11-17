package io.buszi.nascope.snapshot

import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout

/**
 * A snapshot script: one command per line, `#` starts a comment. Commands run in order; the first one that fails
 * stops the run (the shots taken so far stay).
 *
 * Setup (through the app's own use cases, before or after the app is shown):
 * - `demo on|off`: demo mode.
 * - `layout default` / `layout order=<key>,<key>,... hidden=<key>,...`: the stored dashboard layout (either part may
 *   be left out; brick keys as in `DashboardBrickId.key`).
 *
 * App (the app is shown by `start`, or by the first command below):
 * - `start`: shows the app.
 * - `size <width> <height>`: the app's size in dp (Compact < 768, Medium < 1280, Expanded from 1280 with the rail).
 * - `wait <millis>`: lets real time pass while frames are drawn (data loads, polling, animations).
 * - `shot <name>`: writes `<out>/<name>.png`.
 * - `tree <name>`: writes the semantics tree (test tags, texts, states) to `<out>/<name>.txt`.
 * - `click <testTag>` / `clicktext <text>`: clicks the node with that test tag / containing that text.
 * - `scrollto <containerTag> <testTag>`: scrolls the container (e.g. `dashboard_grid`) until the node with that test
 *   tag is composed; lazy lists only compose what is on screen, so `click` and `tree` see nothing below the fold.
 * - `drag <testTag> <dx> <dy>`: drags with the mouse from the node's centre by dx / dy dp.
 * - `key <name>`: presses a key on the focused node (`tab`, `enter`, `space`, `escape`, `up`, `down`, `left`, `right`).
 */
internal object SnapshotScript {

    sealed interface Command {
        val line: Int
    }

    data class Demo(override val line: Int, val enabled: Boolean) : Command
    data class Layout(override val line: Int, val layout: DashboardLayout) : Command
    data class Start(override val line: Int) : Command
    data class Size(override val line: Int, val width: Int, val height: Int) : Command
    data class Wait(override val line: Int, val millis: Long) : Command
    data class Shot(override val line: Int, val name: String) : Command
    data class Tree(override val line: Int, val name: String) : Command
    data class Click(override val line: Int, val tag: String) : Command
    data class ClickText(override val line: Int, val text: String) : Command
    data class ScrollTo(override val line: Int, val containerTag: String, val tag: String) : Command
    data class Drag(override val line: Int, val tag: String, val dx: Float, val dy: Float) : Command
    data class Key(override val line: Int, val name: String) : Command

    val KeyNames = setOf("tab", "enter", "space", "escape", "up", "down", "left", "right")

    /** @throws IllegalArgumentException naming the first line that cannot be read. */
    fun parse(text: String): List<Command> = text.lines().mapIndexedNotNull { index, raw ->
        val line = index + 1
        val content = raw.substringBefore('#').trim()
        if (content.isEmpty()) return@mapIndexedNotNull null
        val words = content.split(Regex("\\s+"))
        val args = words.drop(1)
        fun arity(count: Int) = require(args.size == count) { "line $line: `${words[0]}` takes $count argument(s): $raw" }
        fun int(value: String) = requireNotNull(value.toIntOrNull()?.takeIf { it > 0 }) { "line $line: not a positive number: $value" }
        fun float(value: String) = requireNotNull(value.toFloatOrNull()) { "line $line: not a number: $value" }
        fun name(value: String) = value.also {
            require(it.matches(Regex("[A-Za-z0-9._-]+"))) { "line $line: names use letters, digits, '.', '_' and '-': $it" }
        }

        when (words[0]) {
            "demo" -> {
                arity(1)
                require(args[0] in setOf("on", "off")) { "line $line: demo on|off" }
                Demo(line, args[0] == "on")
            }
            "layout" -> Layout(line, parseLayout(line, args))
            "start" -> Start(line).also { arity(0) }
            "size" -> {
                arity(2)
                Size(line, int(args[0]), int(args[1]))
            }
            "wait" -> {
                arity(1)
                Wait(line, int(args[0]).toLong())
            }
            "shot" -> {
                arity(1)
                Shot(line, name(args[0]))
            }
            "tree" -> {
                arity(1)
                Tree(line, name(args[0]))
            }
            "click" -> {
                arity(1)
                Click(line, args[0])
            }
            "clicktext" -> {
                require(args.isNotEmpty()) { "line $line: clicktext <text>" }
                ClickText(line, content.substringAfter(words[0]).trim())
            }
            "scrollto" -> {
                arity(2)
                ScrollTo(line, args[0], args[1])
            }
            "drag" -> {
                arity(3)
                Drag(line, args[0], float(args[1]), float(args[2]))
            }
            "key" -> {
                arity(1)
                require(args[0] in KeyNames) { "line $line: keys are $KeyNames" }
                Key(line, args[0])
            }
            else -> throw IllegalArgumentException("line $line: unknown command `${words[0]}`")
        }
    }

    private fun parseLayout(line: Int, args: List<String>): DashboardLayout {
        if (args == listOf("default")) return DashboardLayout.Default
        require(args.isNotEmpty()) { "line $line: layout default | layout order=<keys> hidden=<keys>" }
        val parts = args.associate { arg ->
            val key = arg.substringBefore('=', missingDelimiterValue = "")
            require(key == "order" || key == "hidden") { "line $line: expected order=... or hidden=..., got $arg" }
            key to arg.substringAfter('=').split(',').filter { it.isNotBlank() }.map(::DashboardBrickKey)
        }
        return DashboardLayout(order = parts["order"].orEmpty(), hidden = parts["hidden"].orEmpty().toSet())
    }
}
