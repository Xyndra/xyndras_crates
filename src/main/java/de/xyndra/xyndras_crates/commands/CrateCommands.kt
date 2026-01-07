package de.xyndra.xyndras_crates.commands;

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.xyndra.xyndras_crates.lootcrates.CrateConfigManager
import de.xyndra.xyndras_crates.lootcrates.CrateTransformer
import de.xyndra.xyndras_crates.screenhandlers.admin.cratelist.ActiveCrateList
import de.xyndra.xyndras_crates.screenhandlers.admin.cratelist.CrateListAbstractContainerMenu
import de.xyndra.xyndras_crates.util.ParseableMessage
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import java.util.concurrent.CompletableFuture

object CrateCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val padminCommand = literal("crate_admin").requires { source ->
            val player = source.player as? Player
            player != null && (source.hasPermission(2) || isLuckPermsPresent() && getLuckPermsApi()?.userManager?.getUser(
                player.uuid
            )!!.cachedData.permissionData.checkPermission("xyndra.admin.crate").asBoolean()) || source.entity == null
        }

        val crateCommand = literal("crate").requires { source ->
            val player = source.player as? Player
            player != null && (source.hasPermission(2) || isLuckPermsPresent() && getLuckPermsApi()?.userManager?.getUser(
                player.uuid
            )!!.cachedData.permissionData.checkPermission("xyndra.admin.crate")
                .asBoolean()) || source.entity == null
        }.executes { Context ->
            val source = Context.source

            // Open the crate UI
            source.player?.openMenu(SimpleMenuProvider({ syncId, _, p ->
                CrateListAbstractContainerMenu(syncId, p)
            }, Component.literal("Crate Management")))

            1
        }

        val getCrateCommand = literal("getcrate").then(
            Commands.argument("crateName", StringArgumentType.greedyString())
                .suggests { Context, builder -> getCrateNameSuggestions(Context, builder) }
                .executes { Context -> getCrate(Context) },
        )

        val giveKeyCommand = literal("givekey").then(
            Commands.argument("player", EntityArgument.players()).then(
                Commands.argument("amount", IntegerArgumentType.integer(1))
                    .then(Commands.argument("crateName", StringArgumentType.greedyString())
                        .suggests { Context, builder -> getCrateNameSuggestions(Context, builder) }
                        .executes { Context -> giveCrateKey(Context) })
            )
        )


        val activeCrateConfigCommand = literal("activecrates").executes { Context ->
            val source = Context.source

            source.player?.openMenu(SimpleMenuProvider({ syncId, _, p ->
                ActiveCrateList(syncId, p)
            }, Component.literal("Blacklist Particles")))

            1
        }

        val reloadCommand = literal("reload").executes { Context ->
            val source = Context.source
            val crateConfigManager = CrateConfigManager
            crateConfigManager.loadCrateConfigs()
            ParseableMessage("Reloaded crate configs", source.player, "placeholder").send()
            1
        }

        // Register the commands
        dispatcher.register(
            padminCommand.then(crateCommand).then(getCrateCommand).then(giveKeyCommand).then(activeCrateConfigCommand).then(reloadCommand)
        )
    }

    private fun isLuckPermsPresent(): Boolean {
        return try {
            Class.forName("net.luckperms.api.LuckPerms")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun getLuckPermsApi(): LuckPerms? {
        return try {
            LuckPermsProvider.get()
        } catch (e: IllegalStateException) {
            null
        }
    }

    private fun getCrateNameSuggestions(
        Context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val crateConfigManager = CrateConfigManager
        val crateNames = crateConfigManager.loadCrateConfigs().map { it.crateName }
        return SharedSuggestionProvider.suggest(crateNames, builder)
    }

    private fun getCrate(Context: CommandContext<CommandSourceStack>): Int {

        val crateName = StringArgumentType.getString(Context, "crateName")
        val crateTransformer = CrateTransformer(crateName, Context.source.player as Player)

        crateTransformer.giveTransformer()
        return 1
    }


    private fun giveCrateKey(Context: CommandContext<CommandSourceStack>): Int {
        val crateName = StringArgumentType.getString(Context, "crateName")
        val amount = IntegerArgumentType.getInteger(Context, "amount")

        val players = EntityArgument.getPlayers(Context, "player")
        for (player in players) {
            CrateTransformer(crateName, player).giveKey(amount, player)
            val adminMessage = "${player.name.string} received $amount $crateName keys!"

            if (Context.source.player != null) {
                ParseableMessage(adminMessage, Context.source.player, "placeholder").send()
                println(adminMessage)
            }

            println(adminMessage)

        }

        return 1
    }
}
