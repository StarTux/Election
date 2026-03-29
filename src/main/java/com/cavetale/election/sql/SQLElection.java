package com.cavetale.election.sql;

import com.cavetale.election.ElectionType;
import com.winthier.sql.SQLRow;
import io.papermc.paper.dialog.DialogResponseView;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Data
@SQLRow.NotNull
@SQLRow.Name("elections")
public final class SQLElection implements SQLRow {
    @Id private Integer id;
    @VarChar(255) @Unique private String name;
    @VarChar(255) private ElectionType type;
    @VarChar(255) @Nullable private String description;
    @VarChar(255) @Nullable private String permission;
    @VarChar(1024) @Nullable private String displayName;
    private boolean enabled;
    private boolean showVotes;

    public SQLElection() { }

    public SQLElection(final String name, final ElectionType type) {
        this.name = name;
        this.type = type;
    }

    public boolean hasDisplayName() {
        return displayName != null
            && !displayName.isEmpty();
    }

    public Component getDisplayNameComponent() {
        if (displayName == null) return Component.text(name);
        return miniMessage().deserialize(displayName);
    }

    public void load(DialogResponseView response) {
        name = response.getText("name").replace(" ", "");
        type = ElectionType.valueOf(response.getText("type"));
        description = response.getText("description");
        permission = response.getText("permission");
        displayName = response.getText("displayName");
        enabled = response.getBoolean("enabled");
        showVotes = response.getBoolean("showVotes");
    }

    public boolean hasDescription() {
        return description != null
            && !description.isEmpty();
    }

    public Component getDescriptionComponent() {
        if (description == null) return Component.empty();
        return miniMessage().deserialize(description);
    }

    public boolean hasPermission(Player player) {
        return permission == null
            || permission.isEmpty()
            || player.hasPermission(permission);
    }
}
