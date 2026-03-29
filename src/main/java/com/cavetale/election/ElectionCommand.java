package com.cavetale.election;

import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.Connect;
import com.cavetale.election.dialog.ElectionMenuDialog;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.struct.Position;
import com.cavetale.election.util.Json;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class ElectionCommand implements TabExecutor {
    private final ElectionPlugin plugin;

    public void enable() {
        plugin.getCommand("election").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof RemotePlayer player && args.length == 3 && args[0].equals("warp")) {
            warp(player, args[1], args[2]);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(text("/elect: Player expected", RED));
            return true;
        }
        new ElectionMenuDialog(plugin, player).open();
        return true;
    }

    private void warp(RemotePlayer player, String electionName, String choiceName) {
        plugin.database.scheduleAsyncTask(() -> {
                Election election = new Election();
                if (!election.load(electionName)) return;
                if (!election.fill()) return;
                Bukkit.getScheduler().runTask(plugin, () -> warp2(player, election, choiceName));
            });
    }

    private void warp2(RemotePlayer player, Election election, String choiceName) {
        SQLChoice choice = election.findChoice(choiceName);
        if (choice == null) return;
        if (choice.getWarpJson() == null) return;
        Position position = Json.deserialize(choice.getWarpJson(), Position.class);
        if (position == null) return;
        if (!position.isOnThisServer() && player.isPlayer()) {
            Connect.get().dispatchRemoteCommand(player.getPlayer(),
                                                "elect " + election.election.getName() + " warp " + choice.getName(),
                                                position.getServer());
        } else {
            player.bring(plugin, position.toLocation(), player2 -> {
                    if (player2 == null) return;
                    player2.sendMessage(text("Warping to site", GREEN));
                });
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
