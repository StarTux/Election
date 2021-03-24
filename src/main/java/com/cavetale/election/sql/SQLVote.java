package com.cavetale.election.sql;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

/**
 * For ElectionType.UP_DOWN_VOTE.
 */
@Data
@Table(name = "votes",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user", "election_id", "choice_id"})
       })
public final class SQLVote {
    @Id
    private Integer id;
    @Column(nullable = false)
    private UUID user;
    @Column(nullable = true)
    private String userName;
    @Column(nullable = false)
    private int electionId;
    @Column(nullable = false)
    private int choiceId;
    @Column(nullable = false)
    private int value; // -1 = down, +1 = up

    public SQLVote() { }

    public SQLVote(final UUID uuid, final SQLElection election, final SQLChoice choice, final int value) {
        this.user = uuid;
        this.electionId = election.getId();
        this.choiceId = choice.getId();
        this.value = value;
    }
}
