package com.cavetale.election;

import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLVote;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class Books {
    private Books() { }

    public static ItemStack makeBook(Election election, Player player) {
        List<Component> pages = new ArrayList<>();
        TextComponent.Builder frontPage = text();
        frontPage.append(election.election.getDisplayNameComponent());
        if (election.election.getDescription() != null) {
            frontPage.append(Component.newline());
            frontPage.append(Component.newline());
            frontPage.append(text(election.election.getDescription()));
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
        TextComponent.Builder cb = text();
        if (election.election.isShowVotes()) {
            int votes = election.getResults().getOrDefault(choice.getName(), 0);
            String votesString = votes == 1 ? "1 vote" : votes + " votes";
            cb.append(join(separator(space()),
                           text(choice.getName(), DARK_BLUE),
                           text("(" + votesString + ")", BLUE, ITALIC)));
        } else {
            cb.append(text(choice.getName(), DARK_BLUE));
        }
        if (choice.getDescription() != null) {
            cb.append(Component.newline());
            cb.append(text(choice.getDescription()));
        }
        if (choice.getUrl() != null) {
            cb.append(Component.newline());
            cb.append(Component.newline());
            cb.append(text("Link: "));
            cb.append(text("Click here", DARK_BLUE, UNDERLINED)
                      .clickEvent(openUrl(choice.getUrl()))
                      .hoverEvent(showText(text(choice.getUrl(), BLUE))));
        }
        if (choice.getWarpJson() != null) {
            cb.append(Component.newline());
            cb.append(Component.newline());
            cb.append(text("Warp: "));
            String cmd =  "/elect " + election.election.getName() + " warp " + choice.getName();
            cb.append(text("[Click Here]", DARK_GREEN)
                      .clickEvent(runCommand(cmd))
                      .hoverEvent(showText(text("Warp to " + choice.getName()))));
        }
        cb.append(Component.newline());
        cb.append(Component.newline());
        switch (election.election.getType()) {
        case PICK_ONE: {
            SQLBallot ballot = election.findBallot(player.getUniqueId());
            boolean elected = ballot != null && ballot.getChoiceId() == choice.getId();
            if (elected) {
                cb.append(text("\u2612 Voted", GOLD, BOLD));
            } else {
                String cmd =  "/elect " + election.election.getName() + " vote " + choice.getName();
                cb.append(text("\u2610 [Vote]", DARK_BLUE)
                          .hoverEvent(showText(text("Click here to vote", GOLD)))
                          .clickEvent(runCommand(cmd)));
            }
            break;
        }
        case UP_DOWN_VOTE: {
            SQLVote vote = election.findVote(player.getUniqueId(), choice);
            int value = vote != null ? vote.getValue() : 0;
            if (value == 1) {
                String cmd = "/elect " + election.election.getName() + " none " + choice.getName();
                cb.append(text("[Upvote]", GOLD, BOLD)
                          .hoverEvent(showText(text("Changed your mind?", GRAY)))
                          .clickEvent(runCommand(cmd)));
            } else {
                cb.color(DARK_GREEN);
                String cmd =  "/elect " + election.election.getName() + " up " + choice.getName();
                cb.append(text("[Upvote]", DARK_GREEN)
                          .hoverEvent(showText(text("Click here to vote YES", GOLD)))
                          .clickEvent(runCommand(cmd)));
            }
            cb.append(Component.space());
            if (value == -1) {
                String cmd =  "/elect " + election.election.getName() + " none " + choice.getName();
                cb.append(text("[Downvote]", GOLD, BOLD)
                          .hoverEvent(showText(text("Changed your mind?", GRAY)))
                          .clickEvent(runCommand(cmd)));
            } else {
                String cmd =  "/elect " + election.election.getName() + " down " + choice.getName();
                cb.append(text("[Downvote]", DARK_GREEN)
                          .hoverEvent(showText(text("Click here to vote NO", RED)))
                          .clickEvent(runCommand(cmd)));
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
        player.closeInventory();
        player.openBook(make(pages));
    }
}
