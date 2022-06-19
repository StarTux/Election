package com.cavetale.election.sql;

import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import com.winthier.sql.SQLRow;
import lombok.Data;

@Data @NotNull @Name("choices")
@UniqueKey({"election_id", "name"})
public final class SQLChoice implements SQLRow, Comparable<SQLChoice> {
    @Id private Integer id;
    private int electionId;
    @Nullable @VarChar(40) private String name;
    private int priority;
    @Nullable @VarChar(255) private String description;
    @Nullable @VarChar(255) private String url;
    @Nullable @VarChar(255) private String warpJson;

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
