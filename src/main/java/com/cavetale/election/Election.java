package com.cavetale.election;

import com.cavetale.core.command.CommandWarn;
import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLElection;
import com.cavetale.election.sql.SQLVote;
import java.util.List;
import java.util.UUID;

/**
 * An in-memory cache storing everything currently known about an
 * election from the database.
 * Not stored for long.
 */
public final class Election {
    protected SQLElection election;
    protected List<SQLChoice> choices;
    protected List<SQLBallot> ballots;
    protected List<SQLVote> votes;

    public boolean load(String name) {
        election = ElectionPlugin.instance.database.find(SQLElection.class)
            .eq("name", name)
            .findUnique();
        if (election == null) return false;
        return true;
    }

    public boolean fill() {
        choices = ElectionPlugin.instance.database.find(SQLChoice.class)
            .eq("election_id", election.getId())
            .findList();
        switch (election.getType()) {
        case PICK_ONE:
            ballots = ElectionPlugin.instance.database.find(SQLBallot.class)
                .eq("election_id", election.getId())
                .findList();
            break;
        case UP_DOWN_VOTE:
            votes = ElectionPlugin.instance.database.find(SQLVote.class)
                .eq("election_id", election.getId())
                .findList();
            break;
        default:
            break;
        }
        return true;
    }

    public boolean isValid() {
        return election != null
            && choices != null
            && ballots != null;
    }

    public static Election forCommand(String name) {
        Election election = new Election();
        if (!election.load(name)) throw new CommandWarn("Election not found: " + name);
        return election;
    }

    public SQLChoice findChoice(String name) {
        for (SQLChoice choice : choices) {
            if (name.equals(choice.getName())) return choice;
        }
        return null;
    }

    public SQLChoice choiceForCommand(String name) {
        for (SQLChoice choice : choices) {
            if (name.equals(choice.getName())) return choice;
        }
        throw new CommandWarn("Choice not found: " + name);
    }

    public SQLBallot findBallot(UUID uuid) {
        for (SQLBallot ballot : ballots) {
            if (uuid.equals(ballot.getUser())) {
                return ballot;
            }
        }
        return null;
    }

    public SQLVote findVote(UUID uuid, SQLChoice choice) {
        for (SQLVote vote : votes) {
            if (uuid.equals(vote.getUser()) && choice.getId() == vote.getChoiceId()) {
                return vote;
            }
        }
        return null;
    }
}
