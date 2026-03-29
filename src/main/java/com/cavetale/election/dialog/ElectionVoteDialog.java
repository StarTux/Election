package com.cavetale.election.dialog;

import com.cavetale.election.Election;
import com.cavetale.election.ElectionPlugin;
import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLVote;
import com.cavetale.election.struct.Position;
import com.cavetale.mytems.Mytems;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class ElectionVoteDialog {
    private final ElectionPlugin plugin;
    private final Player player;
    private final Election election;

    public void open() {
        player.showDialog(makeDialog(this::createDialogList));
    }

    private Dialog makeDialog(Consumer<DialogRegistryEntry.Builder> consumer) {
        return Dialog.create(factory -> consumer.accept(factory.empty()));
    }

    private void createDialogList(DialogRegistryEntry.Builder builder) {
        final List<DialogBody> bodyList = new ArrayList<>();
        if (election.getElection().hasDescription()) {
            bodyList.add(DialogBody.plainMessage(election.getElection().getDescriptionComponent()));
        }
        final List<Dialog> dialogs = new ArrayList<>();
        for (SQLChoice choice : election.getChoices()) {
            dialogs.add(makeDialog(builder2 -> createChoiceDialog(builder2, choice)));
        }
        builder.base(
            DialogBase.builder(election.getElection().getDisplayNameComponent())
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
                ActionButton.builder(textOfChildren(Mytems.NO, text(" Exit", RED)))
                .build()
            )
            .build()
        );
    }

    private void createChoiceDialog(DialogRegistryEntry.Builder builder, SQLChoice choiceRow) {
        final List<DialogBody> bodyList = new ArrayList<>();
        final Component titlePrefix;
        final List<ActionButton> voteButtons = new ArrayList<>();
        switch (election.getElection().getType()) {
        case PICK_ONE: {
            final SQLBallot ballot = election.findBallot(player.getUniqueId());
            if (ballot == null || ballot.getChoiceId() != choiceRow.getId()) {
                titlePrefix = Mytems.CHECKBOX.asComponent();
                voteButtons.add(
                    ActionButton.builder(textOfChildren(Mytems.CHECKED_CHECKBOX, text(" Vote", GREEN)))
                    .action(
                        DialogAction.customClick(
                            (res, aus) -> pick(choiceRow, ballot),
                            ClickCallback.Options.builder().build()
                        )
                    ).build()
                );
            } else {
                titlePrefix = Mytems.CHECKED_CHECKBOX.asComponent();
                voteButtons.add(
                    ActionButton.builder(textOfChildren(Mytems.CHECKBOX, text(" Undo Vote ", RED)))
                    .action(
                        DialogAction.customClick(
                            (res, aus) -> unpick(choiceRow, ballot),
                            ClickCallback.Options.builder().build()
                        )
                    ).build()
                );
            }
            break;
        }
        case UP_DOWN_VOTE: {
            final SQLVote vote = election.findVote(player.getUniqueId(), choiceRow);
            final boolean upvoted;
            final int voteValue;
            if (vote == null || vote.getValue() == 0) {
                titlePrefix = Mytems.CHECKBOX.asComponent();
                voteValue = 0;
            } else if (vote.getValue() < 0) {
                titlePrefix = Mytems.THUMBS_DOWN.asComponent();
                voteValue = vote.getValue();
            } else {
                titlePrefix = Mytems.THUMBS_UP.asComponent();
                voteValue = vote.getValue();
            }
            if (voteValue < 1) {
                voteButtons.add(
                    ActionButton.builder(textOfChildren(Mytems.THUMBS_UP, text(" Upvote", GREEN)))
                    .action(
                        DialogAction.customClick(
                            (res, aus) -> vote(choiceRow, vote, 1),
                            ClickCallback.Options.builder().build()
                        )
                    ).build()
                );
            }
            if (voteValue > -1) {
                voteButtons.add(
                    ActionButton.builder(textOfChildren(Mytems.THUMBS_DOWN, text(" Downvote", RED)))
                    .action(
                        DialogAction.customClick(
                            (res, aus) -> vote(choiceRow, vote, -1),
                            ClickCallback.Options.builder().build()
                        )
                    ).build()
                );
            }
            if (voteValue != 0) {
                voteButtons.add(
                    ActionButton.builder(textOfChildren(Mytems.CHECKBOX, text(" Undo Vote", RED)))
                    .action(
                        DialogAction.customClick(
                            (res, aus) -> vote(choiceRow, vote, 0),
                            ClickCallback.Options.builder().build()
                        )
                    ).build()
                );
            }
            break;
        }
        default:
            throw new IllegalStateException("type=" + election.getElection().getType());
        }
        if (election.getElection().hasDescription()) {
            bodyList.add(DialogBody.plainMessage(election.getElection().getDescriptionComponent()));
        }
        if (choiceRow.hasDescription()) {
            bodyList.add(DialogBody.plainMessage(choiceRow.getDescriptionComponent()));
        }
        builder.base(
            DialogBase.builder(textOfChildren(titlePrefix, space(), choiceRow.getDisplayNameComponent()))
            .body(bodyList)
            .externalTitle(
                textOfChildren(titlePrefix, space(), choiceRow.getDisplayNameComponent())
            )
            .build()
        );
        final List<ActionButton> actionButtons = new ArrayList<>();
        if (choiceRow.hasUrl()) {
            actionButtons.add(
                ActionButton.builder(text("Open Link", BLUE, UNDERLINED))
                .action(DialogAction.staticAction(openUrl(choiceRow.getUrl())))
                .build()
            );
        }
        if (choiceRow.hasWarp()) {
            final Position warp = choiceRow.getWarpPosition();
            actionButtons.add(
                ActionButton.builder(textOfChildren(Mytems.ARROW_RIGHT, text(" Warp", LIGHT_PURPLE)))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> {
                            if (warp.isOnThisServer()) {
                                player.teleport(warp.toLocation());
                                player.showDialog(makeDialog(builder2 -> createChoiceDialog(builder2, choiceRow)));
                            } else {
                                player.performCommand("elect warp " + election.getElection().getName() + " " + choiceRow.getName());
                            }
                        },
                        ClickCallback.Options.builder().build()
                    )
                ).build()
            );
        }
        actionButtons.addAll(voteButtons);
        builder.type(
            DialogType.multiAction(actionButtons)
            .exitAction(
                ActionButton.builder(textOfChildren(Mytems.TURN_LEFT, text(" Back", GRAY)))
                .action(
                    DialogAction.customClick(
                        (res, aus) -> open(),
                        ClickCallback.Options.builder().build()
                    )
                ).build()
            )
            .columns(1)
            .build()
        );
    }

    /**
     * PICK_ONE.
     */
    private void pick(SQLChoice choice, SQLBallot ballot) {
        if (ballot != null) {
            if (ballot.getChoiceId() == choice.getId()) {
                open();
                return;
            }
            ballot.setChoiceId(choice.getId());
            ballot.setUserName(player.getName());
            plugin.getDatabase().updateAsync(ballot, count -> open());
        } else {
            ballot = new SQLBallot(player.getUniqueId(), player.getName(), election.getElection(), choice);
            election.getBallots().add(ballot);
            plugin.getDatabase().insertIgnoreAsync(ballot, count -> open());
        }
    }

    private void unpick(SQLChoice choice, SQLBallot ballot) {
        if (ballot != null) {
            if (ballot.getChoiceId() == 0) {
                open();
                return;
            }
            ballot.setChoiceId(0);
            ballot.setUserName(player.getName());
            plugin.getDatabase().updateAsync(ballot, count -> open());
        } else {
            open();
        }
    }

    /**
     * UP_DOWN_VOTE.
     */
    private void vote(SQLChoice choice, SQLVote vote, int newValue) {
        if (vote != null) {
            if (vote.getValue() == newValue) return;
            vote.setValue(newValue);
            vote.setUserName(player.getName());
            plugin.getDatabase().updateAsync(vote, count -> open());
        } else {
            vote = new SQLVote(player.getUniqueId(), player.getName(), election.getElection(), choice, newValue);
            election.getVotes().add(vote);
            plugin.getDatabase().insertIgnoreAsync(vote, count -> open());
        }
    }
}
