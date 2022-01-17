package com.cavetale.election;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLElection;
import com.cavetale.election.sql.SQLVote;
import com.cavetale.election.struct.Position;
import com.cavetale.election.util.Json;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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
        rootNode.addChild("list").denyTabCompletion()
            .senderCaller(this::list);
        rootNode.addChild("create").arguments("<name> <type>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.enumLowerList(ElectionType.class))
            .senderCaller(this::create);
        rootNode.addChild("info").arguments("<name>")
            .completers(CommandArgCompleter.supplyList(this::listNames))
            .senderCaller(this::info);
        rootNode.addChild("setdesc").arguments("<name> <desc...>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.EMPTY,
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::setdesc);
        rootNode.addChild("setperm").arguments("<name> <permission>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.EMPTY)
            .senderCaller(this::setperm);
        rootNode.addChild("displayname").arguments("<name> <component>")
            .description("Set the display name components")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.EMPTY)
            .senderCaller(this::displayName);
        CommandNode choiceNode = rootNode.addChild("choice").description("Choice subcommand");
        choiceNode.addChild("create").arguments("<election> <name>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.EMPTY)
            .senderCaller(this::choiceCreate);
        choiceNode.addChild("setdesc").arguments("<election> <choice> <desc...>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.supplyList(this::listChoiceNames),
                        CommandArgCompleter.EMPTY)
            .senderCaller(this::choiceSetdesc);
        choiceNode.addChild("setprio").arguments("<election> <choice> <prio>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.supplyList(this::listChoiceNames),
                        CommandArgCompleter.integer(i -> true))
            .senderCaller(this::choiceSetprio);
        choiceNode.addChild("seturl").arguments("<election> <choice> <url>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.supplyList(this::listChoiceNames),
                        CommandArgCompleter.EMPTY)
            .senderCaller(this::choiceSeturl);
        choiceNode.addChild("setwarp").arguments("<election> <choice>")
            .completers(CommandArgCompleter.supplyList(this::listNames),
                        CommandArgCompleter.supplyList(this::listChoiceNames))
            .playerCaller(this::choiceSetwarp);
        rootNode.addChild("results").arguments("<election>")
            .completers(CommandArgCompleter.supplyList(this::listNames))
            .description("View election results")
            .senderCaller(this::results);
        rootNode.addChild("breakdown").arguments("<election>")
            .completers(CommandArgCompleter.supplyList(this::listNames))
            .description("View election breakdown")
            .senderCaller(this::breakdown);
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

    // Bug: This blocks the main thread!
    private List<String> listNames() {
        List<SQLElection> list = plugin.database.find(SQLElection.class).findList();
        List<String> result = new ArrayList<>();
        for (SQLElection row : list) {
            result.add(row.getName());
        }
        return result;
    }

    // Bug: This blocks the main thread!
    // Bug: This lists all choices!
    private List<String> listChoiceNames() {
        List<SQLChoice> list = plugin.database.find(SQLChoice.class).findList();
        List<String> result = new ArrayList<>();
        for (SQLChoice row : list) {
            if (!result.contains(row.getName())) {
                result.add(row.getName());
            }
        }
        return result;
    }

    boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<SQLElection> list = plugin.database.find(SQLElection.class).findList();
        sender.sendMessage(Component.text(list.size() + " Elections:").color(NamedTextColor.AQUA));
        for (SQLElection row : list) {
            sender.sendMessage(Component.text("  " + row.getName()).color(NamedTextColor.AQUA));
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

    protected boolean displayName(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Election election = Election.forCommand(args[0]);
        String json = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        try {
            election.election.setDisplayNameComponent(GsonComponentSerializer.gson().deserialize(json));
        } catch (Exception e) {
            throw new CommandWarn("Invalid component: " + json);
        }
        if (0 == plugin.database.update(election.election, "displayName")) {
            throw new CommandWarn("Could not update election!");
        }
        sender.sendMessage(Component.text("Display name updated: ", NamedTextColor.AQUA)
                           .append(election.election.getDisplayNameComponent()));
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
            prio = Integer.parseInt(args[2]);
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

    boolean results(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (sender instanceof Player && (!sender.isPermissionSet("election.peek") || !sender.hasPermission("election.peek"))) {
            throw new CommandWarn("You shall not peek!");
        }
        Election election = Election.forCommand(args[0]);
        election.fill();
        Map<String, Integer> results = election.getResults();
        if (results.isEmpty()) throw new CommandWarn("No results: " + election.election.getName());
        sender.sendMessage(Component.text("Election results: " + election.election.getName()).color(NamedTextColor.AQUA));
        List<String> names = new ArrayList<>(results.keySet());
        Collections.sort(names, (b, a) -> Integer.compare(results.get(a), results.get(b)));
        int maxLength = 0;
        Map<String, String> display = new HashMap<>();
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            String value = entry.getValue().toString();
            if (value.length() > maxLength) maxLength = value.length();
            display.put(entry.getKey(), value);
        }
        for (String name : names) {
            StringBuilder sb = new StringBuilder();
            String score = display.get(name);
            int count = maxLength - score.length();
            if (count > 0) {
                for (int i = 0; i < count; i += 1) {
                    sb.append(' ');
                }
                sb.append(score);
                score = sb.toString();
            }
            sender.sendMessage(Component.empty()
                               .append(Component.text("  "))
                               .append(Component.text(score).color(NamedTextColor.YELLOW))
                               .append(Component.text(" "))
                               .append(Component.text(name).color(NamedTextColor.AQUA)));
        }
        return true;
    }

    boolean breakdown(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (sender instanceof Player && (!sender.isPermissionSet("election.peek") || !sender.hasPermission("election.peek"))) {
            throw new CommandWarn("You shall not peek!");
        }
        Election election = Election.forCommand(args[0]);
        election.fill();
        if (election.ballots != null) {
            sender.sendMessage(Component.text(election.ballots.size() + " ballots").color(NamedTextColor.AQUA));
            for (SQLBallot ballot : election.ballots) {
                SQLChoice choice = election.findChoice(ballot.getChoiceId());
                if (choice == null) throw new IllegalStateException("Invalid choice: " + ballot);
                String userName = ballot.getUserName();
                if (userName == null) userName = "?";
                sender.sendMessage(Component.empty()
                                   .append(Component.text("  "))
                                   .append(Component.text(userName)).color(NamedTextColor.AQUA)
                                   .append(Component.text(" "))
                                   .append(Component.text(choice.getName()).color(NamedTextColor.YELLOW)));
            }
        }
        if (election.votes != null) {
            sender.sendMessage(Component.text(election.votes.size() + " votes").color(NamedTextColor.AQUA));
            for (SQLVote vote : election.votes) {
                SQLChoice choice = election.findChoice(vote.getChoiceId());
                if (choice == null) throw new IllegalStateException("Invalid choice: " + vote);
                Component value;
                switch (vote.getValue()) {
                case 1: value = Component.text("+").color(NamedTextColor.GREEN); break;
                case -1: value = Component.text("-").color(NamedTextColor.RED); break;
                case 0: value = Component.text("o").color(NamedTextColor.GRAY); break;
                default: value = Component.text(vote.getValue()).color(NamedTextColor.DARK_RED); break;
                }
                String userName = vote.getUserName();
                if (userName == null) userName = "?";
                sender.sendMessage(Component.empty()
                                   .append(Component.text("  "))
                                   .append(value)
                                   .append(Component.text(" "))
                                   .append(Component.text(userName).color(NamedTextColor.AQUA))
                                   .append(Component.text(" "))
                                   .append(Component.text(choice.getName()).color(NamedTextColor.AQUA)));
            }
        }
        return true;
    }
}
