package com.cavetale.election.sql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "choices",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"election_id", "name"})
       })
public final class SQLChoice implements Comparable<SQLChoice> {
    @Id
    private Integer id;
    @Column(nullable = false)
    private int electionId;
    @Column(nullable = false, length = 255, unique = true)
    private String name;
    @Column(nullable = true)
    private int priority;
    @Column(nullable = true, length = 255)
    private String description;
    @Column(nullable = true, length = 255)
    private String url;
    @Column(nullable = true, length = 255)
    private String warpJson;

    public SQLChoice() { }

    public SQLChoice(final SQLElection parent, final String name) {
        this.electionId = parent.getId();
        this.name = name;
    }

    @Override
    public int compareTo(SQLChoice other) {
        int result = Integer.compare(priority, other.priority);
        return result != 0 ? result : Integer.compare(id, other.id);
    }
}
