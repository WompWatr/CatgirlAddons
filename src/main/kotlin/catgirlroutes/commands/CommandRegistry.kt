package catgirlroutes.commands

import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.nodes.Executable
import com.github.stivais.commodore.nodes.LiteralNode
import com.github.stivais.commodore.utils.findCorrespondingNode
import com.github.stivais.commodore.utils.getArgumentsRequired
import com.github.stivais.commodore.utils.getRootNode
import catgirlroutes.commands.impl.*
import catgirlroutes.utils.rotation.rotationDebug
import me.odinmain.utils.skyblock.modMessage

/**
 * Contains [Commodore] commands to register when the mod is initialized.
 */
object CommandRegistry {

    val commands: ArrayList<Commodore> = arrayListOf(
        catgirlAddonsCommands, devCommands,
        pearlClip, lavaClip, blockClip, aura, inventoryButtons,
        autoP3Commands, autoRoutesCommands, rotationDebug
    )

    fun add(vararg commands: Commodore) {
        commands.forEach { commodore ->
            this.commands.add(commodore)
        }
    }

    fun register() {
        commands.forEach { commodore ->
            commodore.register { problem, cause ->
                val builder = StringBuilder()

                builder.append("§c$problem\n\n")
                builder.append("  Did you mean to run:\n\n")
                buildTreeString(cause, builder)

                findCorrespondingNode(getRootNode(cause), "help")?.let {
                    builder.append("\n  §7Run /${getArgumentsRequired(it).joinToString(" ")} for more help.")
                }
                modMessage(builder.toString())
            }
        }
    }

    private fun buildTreeString(from: LiteralNode, builder: StringBuilder) {
        for (node in from.children) {
            when (node) {
                is LiteralNode -> buildTreeString(node, builder)
                is Executable -> {
                    builder.append("  /${getArgumentsRequired(from).joinToString(" ")}")
                    for (parser in node.parsers) {
                        builder.append(" <${parser.name()}${if (parser.optional()) "?" else ""}>")
                    }
                    builder.append("\n")
                }
            }
        }
    }
}