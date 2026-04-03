package com.cavetale.election.dialog;

import com.cavetale.election.Election;
import com.cavetale.election.ElectionPlugin;
import com.cavetale.election.sql.SQLElection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class ElectionMenuDialog {
    private final ElectionPlugin plugin;
    private final Player player;
    private final List<SQLElection> electionList = new ArrayList<>();

    public void open() {
        plugin.getDatabase().find(SQLElection.class)
            .eq("enabled", 1)
            .findListAsync(this::listLoaded);
    }

    private void listLoaded(List<SQLElection> list) {
        electionList.addAll(list);
        electionList.removeIf(el -> !el.hasPermission(player));
        if (electionList.isEmpty()) {
            player.sendMessage(text("No elections to show", RED));
            return;
        }
        if (electionList.size() == 1) {
            onClick(electionList.get(0));
            return;
        }
        player.showDialog(makeDialog(this::createDialog));
    }

    private Dialog makeDialog(Consumer<DialogRegistryEntry.Builder> consumer) {
        return Dialog.create(factory -> consumer.accept(factory.empty()));
    }

    private void createDialog(DialogRegistryEntry.Builder builder) {
        final List<ActionButton> buttons = new ArrayList<>();
        for (SQLElection electionRow : electionList) {
            buttons.add(
                ActionButton.builder(electionRow.getDisplayNameComponent())
                .action(
                    DialogAction.customClick(
                        (res, aus) -> onClick(electionRow),
                        ClickCallback.Options.builder().build()
                    )
                )
                .build()
            );
        }
        builder.base(
            DialogBase.builder(text("Election List", GREEN, BOLD))
            .build()
        );
        builder.type(
            DialogType.multiAction(buttons)
            .columns(1)
            .build()
        );
    }

    private void onClick(SQLElection electionRow) {
        final Election election = new Election();
        plugin.getDatabase().scheduleAsyncTask(() -> {
                if (!election.load(electionRow.getName())) return;
                if (!election.fill()) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!election.getElection().hasPermission(player)) return;
                        new ElectionVoteDialog(plugin, player, election).open();
                    });
            });
    }
}
