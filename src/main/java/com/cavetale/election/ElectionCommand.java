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
import org.bukkit.Location;
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
        if (args.length == 3 && args[0].equals("warp")) {
            warp(sender, args[1], args[2]);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(text("/elect: Player expected", RED));
            return true;
        }
        new ElectionMenuDialog(plugin, player).open();
        return true;
    }

    private void warp(CommandSender sender, String electionName, String choiceName) {
        plugin.database.scheduleAsyncTask(() -> {
                Election election = new Election();
                if (!election.load(electionName)) return;
                if (!election.fill()) return;
                Bukkit.getScheduler().runTask(plugin, () -> warp2(sender, election, choiceName));
            });
    }

    private void warp2(CommandSender sender, Election election, String choiceName) {
        SQLChoice choice = election.findChoice(choiceName);
        if (choice == null) return;
        if (choice.getWarpJson() == null) return;
        Position position = Json.deserialize(choice.getWarpJson(), Position.class);
        if (position == null) return;
        final Location location = position.toLocation();
        if (location == null) {
            plugin.getLogger().severe("Warp location not found: " + election.getElection().getName() + "/" + choice.getName() + ": " + position);
            return;
        }
        if (!position.isOnThisServer() && sender instanceof Player player) {
            Connect.get().dispatchRemoteCommand(player.getPlayer(),
                                                "elect " + election.election.getName() + " warp " + choice.getName(),
                                                position.getServer());
        } else if (sender instanceof RemotePlayer remote) {
            remote.bring(plugin, location, player -> {
                    if (player == null) return;
                    player.sendMessage(text("Warping to site", GREEN));
                });
        } else if (sender instanceof Player player) {
            player.teleport(location);
            player.sendMessage(text("Warping to site", GREEN));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
