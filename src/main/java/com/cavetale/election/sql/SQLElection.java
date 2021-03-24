package com.cavetale.election.sql;

import com.cavetale.election.ElectionType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "elections")
public final class SQLElection {
    @Id
    private Integer id;
    @Column(nullable = false, length = 255, unique = true)
    private String name;
    @Column(nullable = false, length = 255)
    private ElectionType type;
    @Column(nullable = true, length = 255)
    private String description;
    @Column(nullable = true, length = 255)
    private String permission;

    public SQLElection() { }

    public SQLElection(final String name, final ElectionType type) {
        this.name = name;
        this.type = type;
    }
}
