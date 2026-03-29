package com.cavetale.election.sql;

import com.cavetale.core.util.Json;
import com.cavetale.election.struct.Position;
import com.winthier.sql.SQLRow;
import io.papermc.paper.dialog.DialogResponseView;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Data
@SQLRow.NotNull
@SQLRow.Name("choices")
@SQLRow.UniqueKey({"election_id", "name"})
public final class SQLChoice implements SQLRow, Comparable<SQLChoice> {
    @Id private Integer id;
    private int electionId;
    @Nullable @VarChar(40) private String name;
    @Nullable @Text private String displayName;
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

    public boolean hasDisplayName() {
        return displayName != null
            && !displayName.isEmpty();
    }

    public Component getDisplayNameComponent() {
        if (displayName == null) return Component.text(name);
        return miniMessage().deserialize(displayName);
    }

    public Position getWarpPosition() {
        if (warpJson == null) return null;
        return Json.deserialize(warpJson, Position.class);
    }

    public void setWarpLocation(Location location) {
        warpJson = Json.serialize(new Position(location));
    }

    public void load(DialogResponseView response) {
        name = response.getText("name").replace(" ", "");
        displayName = response.getText("displayName");
        priority = response.getFloat("priority").intValue();
        description = response.getText("description");
        url = response.getText("url");
    }

    public boolean hasDescription() {
        return description != null
            && !description.isEmpty();
    }

    public Component getDescriptionComponent() {
        if (description == null) return Component.empty();
        return miniMessage().deserialize(description);
    }

    public boolean hasUrl() {
        return url != null
            && !url.isEmpty();
    }

    public boolean hasWarp() {
        return warpJson != null
            && !warpJson.isEmpty();
    }
}
