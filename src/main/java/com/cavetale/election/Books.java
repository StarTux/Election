package com.cavetale.election;

import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLVote;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public final class Books {
    private Books() { }

    public static ItemStack makeBook(Election election, Player player) {
        List<Component> pages = new ArrayList<>();
        TextComponent.Builder frontPage = Component.text();
        frontPage.append(election.election.getDisplayNameComponent());
        if (election.election.getDescription() != null) {
            frontPage.append(Component.newline());
            frontPage.append(Component.newline());
            frontPage.append(Component.text(election.election.getDescription()));
        }
        pages.add(frontPage.build());
        for (SQLChoice choice : election.choices) {
            pages.add(makeChoicePage(election, choice, player));
        }
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        item.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                meta.setTitle("Election");
                meta.setAuthor("Cavetale");
                meta.setGeneration(BookMeta.Generation.ORIGINAL);
                meta.pages(pages);
            });
        return item;
    }

    public static Component makeChoicePage(Election election, SQLChoice choice, Player player) {
        TextComponent.Builder cb = Component.text();
        cb.append(Component.text(choice.getName(), NamedTextColor.DARK_BLUE));
        if (choice.getDescription() != null) {
            cb.append(Component.newline());
            cb.append(Component.text(choice.getDescription()));
        }
        if (choice.getUrl() != null) {
            cb.append(Component.newline());
            cb.append(Component.newline());
            cb.append(Component.text("Link: "));
            cb.append(Component.text("Click here", NamedTextColor.DARK_BLUE, TextDecoration.UNDERLINED)
                      .clickEvent(ClickEvent.openUrl(choice.getUrl()))
                      .hoverEvent(HoverEvent.showText(Component.text(choice.getUrl(), NamedTextColor.BLUE))));
        }
        if (choice.getWarpJson() != null) {
            cb.append(Component.newline());
            cb.append(Component.newline());
            cb.append(Component.text("Warp: "));
            String cmd =  "/elect " + election.election.getName() + " warp " + choice.getName();
            cb.append(Component.text("[Click Here]", NamedTextColor.DARK_GREEN)
                      .clickEvent(ClickEvent.runCommand(cmd))
                      .hoverEvent(HoverEvent.showText(Component.text("Warp to " + choice.getName()))));
        }
        cb.append(Component.newline());
        cb.append(Component.newline());
        switch (election.election.getType()) {
        case PICK_ONE: {
            SQLBallot ballot = election.findBallot(player.getUniqueId());
            boolean elected = ballot != null && ballot.getChoiceId() == choice.getId();
            if (elected) {
                cb.append(Component.text("\u2612 Voted", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else {
                String cmd =  "/elect " + election.election.getName() + " vote " + choice.getName();
                cb.append(Component.text("\u2610 [Vote]", NamedTextColor.DARK_BLUE)
                          .hoverEvent(HoverEvent.showText(Component.text("Click here to vote", NamedTextColor.GOLD)))
                          .clickEvent(ClickEvent.runCommand(cmd)));
            }
            break;
        }
        case UP_DOWN_VOTE: {
            SQLVote vote = election.findVote(player.getUniqueId(), choice);
            int value = vote != null ? vote.getValue() : 0;
            if (value == 1) {
                String cmd = "/elect " + election.election.getName() + " none " + choice.getName();
                cb.append(Component.text("[Upvote]", NamedTextColor.GOLD, TextDecoration.BOLD)
                          .hoverEvent(HoverEvent.showText(Component.text("Changed your mind?", NamedTextColor.GRAY)))
                          .clickEvent(ClickEvent.runCommand(cmd)));
            } else {
                cb.color(NamedTextColor.DARK_GREEN);
                String cmd =  "/elect " + election.election.getName() + " up " + choice.getName();
                cb.append(Component.text("[Upvote]", NamedTextColor.DARK_GREEN)
                          .hoverEvent(HoverEvent.showText(Component.text("Click here to vote YES", NamedTextColor.GOLD)))
                          .clickEvent(ClickEvent.runCommand(cmd)));
            }
            cb.append(Component.space());
            if (value == -1) {
                String cmd =  "/elect " + election.election.getName() + " none " + choice.getName();
                cb.append(Component.text("[Downvote]", NamedTextColor.GOLD, TextDecoration.BOLD)
                          .hoverEvent(HoverEvent.showText(Component.text("Changed your mind?", NamedTextColor.GRAY)))
                          .clickEvent(ClickEvent.runCommand(cmd)));
            } else {
                String cmd =  "/elect " + election.election.getName() + " down " + choice.getName();
                cb.append(Component.text("[Downvote]", NamedTextColor.DARK_GREEN)
                          .hoverEvent(HoverEvent.showText(Component.text("Click here to vote NO", NamedTextColor.RED)))
                          .clickEvent(ClickEvent.runCommand(cmd)));
            }
            break;
        }
        default:
            throw new IllegalStateException("electionType=" + election.election.getType());
        }
        return cb.build();
    }

    public static ItemStack make(List<Component> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (m instanceof BookMeta meta) {
                    meta.setAuthor("Cavetale");
                    meta.title(Component.empty());
                    meta.pages(pages);
                }
            });
        return book;
    }

    public static void open(Player player, List<Component> pages) {
        player.openBook(make(pages));
    }
}
