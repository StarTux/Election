package com.cavetale.election;

import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLVote;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public final class Books {
    private Books() { }

    public static ItemStack makeBook(Election election, Player player) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.setTitle("Election");
        meta.setAuthor("Cavetale");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        //
        ComponentBuilder cb = new ComponentBuilder();
        cb.append(election.election.getName()).color(ChatColor.DARK_BLUE);
        if (election.election.getDescription() != null) {
            cb.append("\n\n").reset();
            cb.append(election.election.getDescription());
        }
        meta.spigot().addPage(cb.create());
        for (SQLChoice choice : election.choices) {
            cb = new ComponentBuilder();
            cb.append(choice.getName()).color(ChatColor.DARK_BLUE);
            if (choice.getDescription() != null) {
                cb.append("\n").reset();
                cb.append(choice.getDescription());
            }
            if (choice.getUrl() != null) {
                cb.append("\n\n").reset();
                cb.append("Link: ").color(ChatColor.BLACK);
                cb.append("Click here").color(ChatColor.DARK_BLUE).underlined(true)
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, choice.getUrl()));
            }
            if (choice.getWarpJson() != null) {
                cb.append("\n\n").reset();
                cb.append("Warp: ").color(ChatColor.BLACK);
                String cmd =  "/elect " + election.election.getName() + " warp " + choice.getName();
                cb.append("[Click Here]").color(ChatColor.DARK_GREEN)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
            }
            cb.append("\n\n").reset();
            switch (election.election.getType()) {
            case PICK_ONE: {
                SQLBallot ballot = election.findBallot(player.getUniqueId());
                boolean elected = ballot != null && ballot.getChoiceId() == choice.getId();
                if (elected) {
                    cb.append("\u2610 [Vote]");
                    cb.color(ChatColor.DARK_BLUE).bold(true);
                    cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            TextComponent.fromLegacyText(ChatColor.GOLD + "Click here to vote")));
                } else {
                    cb.append("\u2612 Voted");
                    cb.color(ChatColor.DARK_GREEN);
                    String cmd =  "/elect " + election.election.getName() + " vote " + choice.getName();
                    cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                }
                break;
            }
            case UP_DOWN_VOTE: {
                SQLVote vote = election.findVote(player.getUniqueId(), choice);
                int value = vote != null ? vote.getValue() : 0;
                cb.append("[Upvote]");
                if (value == 1) {
                    cb.color(ChatColor.GOLD).bold(true);
                    String cmd =  "/elect " + election.election.getName() + " none " + choice.getName();
                    cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                } else {
                    cb.color(ChatColor.DARK_GREEN);
                    String cmd =  "/elect " + election.election.getName() + " up " + choice.getName();
                    cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                }
                cb.append(" ").reset();
                cb.append("[Downvote]");
                if (value == -1) {
                    cb.color(ChatColor.GOLD).bold(true);
                    String cmd =  "/elect " + election.election.getName() + " none " + choice.getName();
                    cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                } else {
                    cb.color(ChatColor.DARK_GREEN);
                    String cmd =  "/elect " + election.election.getName() + " down " + choice.getName();
                    cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                }
                break;
            }
            default:
                break;
            }
            meta.spigot().addPage(cb.create());
        }
        //
        item.setItemMeta(meta);
        return item;
    }
}
