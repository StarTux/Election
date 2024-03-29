package com.cavetale.election.sql;

import com.winthier.sql.SQLRow;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

/**
 * For ElectionType.PICK_ONE.
 */
@Data
@Table(name = "ballots",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user", "election_id"})
       })
public final class SQLBallot implements SQLRow {
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

    public SQLBallot() { }

    public SQLBallot(final UUID uuid, final String userName, final SQLElection election, final SQLChoice choice) {
        this.user = uuid;
        this.userName = userName;
        this.electionId = election.getId();
        this.choiceId = choice.getId();
    }
}
