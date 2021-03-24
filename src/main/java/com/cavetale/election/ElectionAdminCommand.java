package com.cavetale.election;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLElection;
import com.cavetale.election.sql.SQLVote;
import com.cavetale.election.struct.Position;
import com.cavetale.election.util.Json;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class ElectionAdminCommand implements TabExecutor {
    private final ElectionPlugin plugin;
    private CommandNode rootNode;

    public void enable() {
        rootNode = new CommandNode("electionadmin");
        rootNode.addChild("list").denyTabCompletion().senderCaller(this::list);
        rootNode.addChild("create").arguments("<name> <type>").senderCaller(this::create);
        rootNode.addChild("info").arguments("<name>").senderCaller(this::info);
        rootNode.addChild("setdesc").arguments("<name> <desc...>").senderCaller(this::setdesc);
        rootNode.addChild("setperm").arguments("<name> <permission>").senderCaller(this::setperm);
        CommandNode choiceNode = rootNode.addChild("choice").description("Choice subcommand");
        choiceNode.addChild("create").arguments("<election> <name>").senderCaller(this::choiceCreate);
        choiceNode.addChild("setdesc").arguments("<election> <choice> <desc...>").senderCaller(this::choiceSetdesc);
        choiceNode.addChild("setprio").arguments("<election> <choice> <prio>").senderCaller(this::choiceSetprio);
        choiceNode.addChild("seturl").arguments("<election> <choice> <url>").senderCaller(this::choiceSeturl);
        choiceNode.addChild("setwarp").arguments("<election> <choice>").playerCaller(this::choiceSetwarp);
        plugin.getCommand("electionadmin").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return rootNode.call(sender, command, alias, args);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return rootNode.complete(sender, command, alias, args);
    }

    boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<SQLElection> list = plugin.database.find(SQLElection.class).findList();
        sender.sendMessage(Component.text(list.size() + " Elections:").color(NamedTextColor.AQUA));
        for (SQLElection row : list) {
            sender.sendMessage(Component.text("- " + row.toString()).color(NamedTextColor.AQUA));
        }
        return true;
    }

    boolean create(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String name = args[0];
        ElectionType electionType;
        try {
            electionType = ElectionType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Illegal election type: " + args[1]);
        }
        SQLElection row = new SQLElection(name, electionType);
        if (0 == plugin.database.insertIgnore(row)) {
            throw new CommandWarn("Election already exists: " + name);
        }
        sender.sendMessage(Component.text("Election created: " + name).color(NamedTextColor.AQUA));
        return true;
    }

    boolean info(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Election election = Election.forCommand(args[0]);
        election.fill();
        sender.sendMessage(Component.text(election.election.toString()).color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Choices: " + election.choices.size()).color(NamedTextColor.AQUA));
        for (SQLChoice row : election.choices) {
            sender.sendMessage(Component.text("- " + row.toString()).color(NamedTextColor.AQUA));
        }
        if (election.ballots != null) {
            sender.sendMessage(Component.text("Ballots: " + election.ballots.size()).color(NamedTextColor.AQUA));
            for (SQLBallot row : election.ballots) {
                sender.sendMessage(Component.text("- " + row.toString()).color(NamedTextColor.AQUA));
            }
        }
        if (election.votes != null) {
            sender.sendMessage(Component.text("Votes: " + election.votes.size()).color(NamedTextColor.AQUA));
            for (SQLVote row : election.votes) {
                sender.sendMessage(Component.text("- " + row.toString()).color(NamedTextColor.AQUA));
            }
        }
        return true;
    }

    boolean setdesc(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Election election = Election.forCommand(args[0]);
        election.election.setDescription(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        if (0 == plugin.database.update(election.election, "description")) {
            throw new CommandWarn("Could not update election!");
        }
        sender.sendMessage(Component.text("Description updated " + election.election.getDescription()).color(NamedTextColor.AQUA));
        return true;
    }

    boolean setperm(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        Election election = Election.forCommand(args[0]);
        election.election.setPermission(args.length >= 2 ? args[1] : null);
        if (0 == plugin.database.update(election.election, "permission")) {
            throw new CommandWarn("Could not update election!");
        }
        sender.sendMessage(Component.text("Permission updated " + election.election.getPermission()).color(NamedTextColor.AQUA));
        return true;
    }

    boolean choiceCreate(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        Election election = Election.forCommand(args[0]);
        SQLChoice choice = new SQLChoice(election.election, args[1]);
        if (0 == plugin.database.insertIgnore(choice)) {
            throw new CommandWarn("Choice already exists: " + choice.getName());
        }
        sender.sendMessage(Component.text("Choice created: " + choice.getName()).color(NamedTextColor.AQUA));
        return true;
    }

    boolean choiceSetdesc(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        Election election = Election.forCommand(args[0]);
        election.fill();
        SQLChoice choice = election.choiceForCommand(args[1]);
        choice.setDescription(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        if (0 == plugin.database.update(choice, "description")) {
            throw new CommandWarn("Could not update election!");
        }
        sender.sendMessage(Component.text("Description updated: " + choice.getDescription()).color(NamedTextColor.AQUA));
        return true;
    }

    boolean choiceSetprio(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        Election election = Election.forCommand(args[0]);
        election.fill();
        SQLChoice choice = election.choiceForCommand(args[1]);
        int prio;
        try {
            prio = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Number expected: " + args[2]);
        }
        choice.setPriority(prio);
        if (0 == plugin.database.update(choice, "priority")) {
            throw new CommandWarn("Could not update choice!");
        }
        sender.sendMessage(Component.text("Priority updated: " + choice.getPriority()).color(NamedTextColor.AQUA));
        return true;
    }

    boolean choiceSeturl(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        Election election = Election.forCommand(args[0]);
        election.fill();
        SQLChoice choice = election.choiceForCommand(args[1]);
        choice.setUrl(args[2]);
        if (0 == plugin.database.update(choice, "url")) {
            throw new CommandWarn("Could not update election!");
        }
        sender.sendMessage(Component.text("URL updated: " + choice.getUrl()).color(NamedTextColor.AQUA));
        return true;
    }

    boolean choiceSetwarp(Player player, String[] args) {
        if (args.length != 2) return false;
        Election election = Election.forCommand(args[0]);
        election.fill();
        SQLChoice choice = election.choiceForCommand(args[1]);
        Position position = new Position(player.getLocation());
        choice.setWarpJson(Json.serialize(position));
        if (0 == plugin.database.update(choice, "warp_json")) {
            throw new CommandWarn("Could not update election!");
        }
        player.sendMessage(Component.text("Warp updated: " + choice.getWarpJson()).color(NamedTextColor.AQUA));
        return true;
    }
}
