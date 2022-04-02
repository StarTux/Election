package com.cavetale.election;

import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLElection;
import com.cavetale.election.sql.SQLVote;
import com.cavetale.election.struct.Position;
import com.cavetale.election.util.Json;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class ElectionCommand implements TabExecutor {
    private final ElectionPlugin plugin;

    public void enable() {
        plugin.getCommand("election").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[election:elect] Player expected!");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.database.find(SQLElection.class).findListAsync(list -> {
                    list.removeIf(row -> !row.isEnabled());
                    list.removeIf(row -> row.getPermission() != null
                                  && !row.getPermission().isEmpty()
                                  && !player.hasPermission(row.getPermission()));
                    if (list.isEmpty()) {
                        player.sendMessage(text("No elections available!", RED));
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(text("Elections", DARK_BLUE, BOLD));
                    lines.add(newline());
                    for (SQLElection row : list) {
                        String cmd = "/elect " + row.getName();
                        Component displayName = row.getDisplayNameComponent();
                        lines.add(displayName
                                  .clickEvent(ClickEvent.runCommand(cmd))
                                  .hoverEvent(HoverEvent.showText(displayName)));
                    }
                    Books.open(player, List.of(join(separator(newline()), lines)));
                });
            return true;
        }
        plugin.database.scheduleAsyncTask(() -> {
                Election election = new Election();
                if (!election.load(args[0])) return;
                if (!election.fill()) return;
                Bukkit.getScheduler().runTask(plugin, () -> onCommand(player, election, Arrays.copyOfRange(args, 1, args.length)));
            });
        return true;
    }

    void openBook(Player player, String name) {
        plugin.database.scheduleAsyncTask(() -> {
                Election election = new Election();
                if (!election.load(name)) return;
                if (!election.fill()) return;
                Bukkit.getScheduler().runTask(plugin, () -> player.openBook(Books.makeBook(election, player)));
            });
    }

    private void onCommand(Player player, Election election, String[] args) {
        if (!election.election.isEnabled()) return;
        String permission = election.election.getPermission();
        if (permission != null && !permission.isEmpty()) {
            if (!player.hasPermission(permission)) return;
        }
        if (args.length == 0) {
            player.openBook(Books.makeBook(election, player));
            return;
        }
        switch (args[0]) {
        case "vote": {
            if (args.length != 2) return;
            SQLChoice choice = election.findChoice(args[1]);
            if (choice == null) return;
            if (election.election.getType() != ElectionType.PICK_ONE) return;
            SQLBallot ballot = election.findBallot(player.getUniqueId());
            if (ballot != null) {
                if (ballot.getChoiceId() == choice.getId()) return;
                ballot.setChoiceId(choice.getId());
                ballot.setUserName(player.getName());
                plugin.database.updateAsync(ballot, count -> voteCallback(player, count, election), "choice_id", "user_name");
            } else {
                ballot = new SQLBallot(player.getUniqueId(), player.getName(), election.election, choice);
                plugin.database.insertIgnoreAsync(ballot, count -> voteCallback(player, count, election));
            }
            break;
        }
        case "up":
        case "down":
        case "none": {
            if (args.length != 2) return;
            SQLChoice choice = election.findChoice(args[1]);
            if (choice == null) return;
            if (election.election.getType() != ElectionType.UP_DOWN_VOTE) return;
            int newValue;
            switch (args[0]) {
            case "up": newValue = 1; break;
            case "down": newValue = -1; break;
            default: newValue = 0; break;
            }
            SQLVote vote = election.findVote(player.getUniqueId(), choice);
            if (vote != null) {
                if (vote.getValue() == newValue) return;
                vote.setValue(newValue);
                vote.setUserName(player.getName());
                plugin.database.updateAsync(vote, count -> voteCallback(player, count, election), "value", "user_name");
            } else {
                vote = new SQLVote(player.getUniqueId(), player.getName(), election.election, choice, newValue);
                plugin.database.insertIgnoreAsync(vote, count -> voteCallback(player, count, election));
            }
            break;
        }
        case "warp": {
            if (args.length != 2) return;
            SQLChoice choice = election.findChoice(args[1]);
            if (choice == null) return;
            if (choice.getWarpJson() == null) return;
            Position position = Json.deserialize(choice.getWarpJson(), Position.class);
            if (position == null) return;
            player.teleport(position.toLocation(), TeleportCause.PLUGIN);
            player.sendMessage(text("Warping to site").color(GREEN));
            break;
        }
        default: break;
        }
    }

    private void voteCallback(Player player, int count, Election election) {
        if (count == 0) {
            player.sendMessage(text("Vote failed!").color(RED));
        } else {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
            player.sendMessage(text("Vote succeeded!").color(GREEN));
            openBook(player, election.election.getName());
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return Collections.emptyList();
    }
}
