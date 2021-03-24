package com.cavetale.election;

import com.cavetale.election.sql.SQLBallot;
import com.cavetale.election.sql.SQLChoice;
import com.cavetale.election.sql.SQLElection;
import com.cavetale.election.sql.SQLVote;
import com.winthier.sql.SQLDatabase;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElectionPlugin extends JavaPlugin {
    protected static ElectionPlugin instance;
    private ElectionCommand electionCommand = new ElectionCommand(this);
    private ElectionAdminCommand electionAdminCommand = new ElectionAdminCommand(this);
    private EventListener eventListener = new EventListener(this);
    protected SQLDatabase database;

    @Override
    public void onEnable() {
        instance = this;
        database = new SQLDatabase(this);
        database.registerTables(SQLElection.class, SQLChoice.class, SQLBallot.class, SQLVote.class);
        if (!database.createAllTables()) {
            throw new IllegalStateException("Database setup failed!");
        }
        electionCommand.enable();
        electionAdminCommand.enable();
        eventListener.enable();
    }

    @Override
    public void onDisable() {
        database.waitForAsyncTask();
        database.close();
    }
}
