package com.cavetale.election.dialog;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.election.Election;
import com.cavetale.election.ElectionPlugin;
import com.cavetale.election.ElectionType;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLElection;
import com.cavetale.election.struct.Position;
import com.cavetale.mytems.Mytems;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class ElectionEditDialog {
    private final ElectionPlugin plugin;
    private final Player editor;
    private final Election election;

    public boolean isCreating() {
        return election.getElection().getId() == null;
    }

    public void open() {
        editor.showDialog(makeDialog(this::createDialogList));
    }

    private Dialog makeDialog(Consumer<DialogRegistryEntry.Builder> consumer) {
        return Dialog.create(factory -> consumer.accept(factory.empty()));
    }

    private void createDialogList(DialogRegistryEntry.Builder builder) {
        final List<DialogBody> bodyList = new ArrayList<>();
        if (isCreating()) {
            bodyList.add(DialogBody.plainMessage(text("New Election", GREEN)));
        } else {
            bodyList.add(
                DialogBody.plainMessage(
                    textOfChildren(
                        text("Election id #", GRAY),
                        text(election.getElection().getId(), YELLOW)
                    )
                )
            );
        }
        final List<Dialog> dialogs = new ArrayList<>();
        dialogs.add(makeDialog(this::createElectionDialog));
        for (SQLChoice choice : election.getChoices()) {
            dialogs.add(makeDialog(builder2 -> createChoiceDialog(builder2, choice)));
        }
        if (!isCreating()) {
            SQLChoice newChoice = new SQLChoice();
            newChoice.setElectionId(election.getElection().getId());
            dialogs.add(makeDialog(builder2 -> createChoiceDialog(builder2, newChoice)));
        }
        builder.base(
            DialogBase.builder(text("Election"))
            .body(bodyList)
            .build()
        );
        builder.type(
            DialogType.dialogList(
                RegistrySet.valueSet(
                    RegistryKey.DIALOG,
                    dialogs
                )
            )
            .columns(1)
            .exitAction(
                ActionButton.builder(text("Cancel", RED))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> cancel(),
                        ClickCallback.Options.builder()
                        .uses(1)
                        .build()
                    )
                ).build()
            )
            .build()
        );
    }

    private void createElectionDialog(DialogRegistryEntry.Builder builder) {
        final SQLElection electionRow = election.getElection();
        final List<DialogBody> bodyList = new ArrayList<>();
        if (isCreating()) {
            bodyList.add(DialogBody.plainMessage(text("New Election", GREEN)));
        } else {
            bodyList.add(
                DialogBody.plainMessage(
                    textOfChildren(
                        text("Election id #", GRAY),
                        text(election.getElection().getId(), YELLOW)
                    )
                )
            );
        }
        final List<DialogInput> inputList = new ArrayList<>();
        inputList.add(
            DialogInput.text("name", text("Name"))
            .initial(electionRow.getName() != null ? electionRow.getName() : "")
            .maxLength(255)
            .build()
        );
        inputList.add(
            DialogInput.singleOption(
                "type",
                text("Type"),
                Arrays.stream(ElectionType.values())
                .map(
                    en -> SingleOptionDialogInput.OptionEntry.create(
                        en.name(),
                        text(en.getDisplayName()),
                        en.ordinal() == 0
                    )
                ).toList()
            ).build()
        );
        inputList.add(
            DialogInput.text("displayName", text("Display Name"))
            .initial(electionRow.getDisplayName() != null ? electionRow.getDisplayName() : "")
            .maxLength(1024)
            .build()
        );
        inputList.add(
            DialogInput.text("description", text("Description"))
            .initial(electionRow.getDescription() != null ? electionRow.getDescription() : "")
            .maxLength(255)
            .build()
        );
        inputList.add(
            DialogInput.text("permission", text("Permission"))
            .initial(electionRow.getPermission() != null ? electionRow.getPermission() : "")
            .maxLength(255)
            .build()
        );
        inputList.add(
            DialogInput.bool("enabled", text("Enabled"))
            .initial(electionRow.isEnabled())
            .onTrue("Yes")
            .onFalse("No")
            .build()
        );
        inputList.add(
            DialogInput.bool("showVotes", text("Show Votes"))
            .initial(electionRow.isEnabled())
            .onTrue("Yes")
            .onFalse("No")
            .build()
        );
        builder.base(
            DialogBase.builder(text("Election"))
            .body(bodyList)
            .inputs(inputList)
            .externalTitle(textOfChildren(text("[Election] ", GRAY), electionRow.getDisplayNameComponent()))
            .build()
        );
        builder.type(
            DialogType.confirmation(
                ActionButton.builder(text("Save"))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> {
                            saveElection(res);
                            open();
                        },
                        ClickCallback.Options.builder()
                        .uses(1)
                        .build()
                    )
                ).build(),
                ActionButton.builder(textOfChildren(Mytems.TURN_LEFT, text(" Back", RED)))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> open(),
                        ClickCallback.Options.builder()
                        .uses(1)
                        .build()
                    )
                ).build()
            )
        );
    }

    private void createChoiceDialog(DialogRegistryEntry.Builder builder, SQLChoice choiceRow) {
        final List<DialogBody> bodyList = new ArrayList<>();
        bodyList.add(
            DialogBody.plainMessage(
                textOfChildren(
                    text("Election id #", GRAY),
                    text("" + election.getElection().getId(), YELLOW)
                )
            )
        );
        if (choiceRow.getId() == null) {
            bodyList.add(DialogBody.plainMessage(text("New Choice", GREEN)));
        } else {
            bodyList.add(
                DialogBody.plainMessage(
                    textOfChildren(
                        text("Choice id #", GRAY),
                        text(choiceRow.getId(), YELLOW)
                    )
                )
            );
        }
        final Position warp = choiceRow.getWarpPosition();
        if (warp != null) {
            bodyList.add(
                DialogBody.plainMessage(
                    textOfChildren(
                        text("Warp:", GRAY),
                        text(" " + warp.getServer()),
                        text(" " + warp.getWorld()),
                        text(" " + warp.getBlockX()),
                        text(" " + warp.getBlockY()),
                        text(" " + warp.getBlockZ())
                    )
                )
            );
        }
        final List<DialogInput> inputList = new ArrayList<>();
        inputList.add(
            DialogInput.text("name", text("Name"))
            .initial(choiceRow.getName() != null ? choiceRow.getName() : "")
            .maxLength(40)
            .build()
        );
        inputList.add(
            DialogInput.text("displayName", text("Display Name"))
            .initial(choiceRow.getDisplayName() != null ? choiceRow.getDisplayName() : "")
            .maxLength(4096)
            .build()
        );
        inputList.add(
            DialogInput.numberRange("priority", text("Priority"), -1000f, 1000f)
            .initial((float) choiceRow.getPriority())
            .step(1f)
            .build()
        );
        inputList.add(
            DialogInput.text("description", text("Description"))
            .initial(choiceRow.getDescription() != null ? choiceRow.getDescription() : "")
            .maxLength(255)
            .build()
        );
        inputList.add(
            DialogInput.text("url", text("URL"))
            .initial(choiceRow.getUrl() != null ? choiceRow.getUrl() : "")
            .maxLength(255)
            .build()
        );
        inputList.add(
            DialogInput.bool("copyLocation", text("Copy Warp Location").hoverEvent(text("hi")))
            .initial(false)
            .build()
        );
        builder.base(
            DialogBase.builder(text("Choice"))
            .body(bodyList)
            .inputs(inputList)
            .externalTitle(
                choiceRow.getId() != null
                ? textOfChildren(text("[Choice] ", GRAY), choiceRow.getDisplayNameComponent())
                : textOfChildren(VanillaItems.EGG, text(" New Choice", GREEN))
            )
            .build()
        );
        builder.type(
            DialogType.confirmation(
                ActionButton.builder(text("Save"))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> {
                            saveChoice(res, choiceRow);
                            open();
                        },
                        ClickCallback.Options.builder()
                        .uses(1)
                        .build()
                    )
                ).build(),
                ActionButton.builder(textOfChildren(Mytems.TURN_LEFT, text(" Back", RED)))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> open(),
                        ClickCallback.Options.builder()
                        .uses(1)
                        .build()
                    )
                ).build()
            )
        );
    }

    private void saveElection(DialogResponseView response) {
        final SQLElection electionRow = election.getElection();
        electionRow.load(response);
        final int saveElectionResult = electionRow.getId() == null
            ? plugin.getDatabase().insert(electionRow)
            : plugin.getDatabase().update(electionRow);
        if (saveElectionResult == 0) {
            editor.sendMessage(text("Election saving failed!", DARK_RED));
        } else {
            editor.sendMessage(
                textOfChildren(
                    text("Election saved: ", YELLOW),
                    electionRow.getDisplayNameComponent()
                )
            );
        }
    }

    private void saveChoice(DialogResponseView response, SQLChoice choiceRow) {
        choiceRow.load(response);
        if (response.getBoolean("copyLocation")) {
            choiceRow.setWarpLocation(editor.getLocation());
        }
        if (choiceRow.getId() == null) {
            election.getChoices().add(choiceRow);
        }
        final int saveChoiceResult = choiceRow.getId() == null
            ? plugin.getDatabase().insert(choiceRow)
            : plugin.getDatabase().update(choiceRow);
        if (saveChoiceResult == 0) {
            editor.sendMessage(text("Choice saving failed!", DARK_RED));
            return;
        } else {
            editor.sendMessage(
                textOfChildren(
                    text("Choice saved: ", YELLOW),
                    choiceRow.getDisplayNameComponent()
                )
            );
        }
        Collections.sort(election.getChoices());
        open();
    }

    private void cancel() {
        editor.sendMessage(text("Election editing cancelled", RED));
    }
}
