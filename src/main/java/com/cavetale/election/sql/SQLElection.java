package com.cavetale.election.sql;

import com.cavetale.election.ElectionType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

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
    @Column(nullable = true, length = 1024)
    private String displayName;
    @Column(nullable = false)
    private boolean enabled;
    @Column(nullable = false)
    private boolean showVotes;

    public SQLElection() { }

    public SQLElection(final String name, final ElectionType type) {
        this.name = name;
        this.type = type;
    }

    public Component getDisplayNameComponent() {
        if (displayName == null) return Component.text(name);
        try {
            return GsonComponentSerializer.gson().deserialize(displayName);
        } catch (Exception e) {
            e.printStackTrace();
            return Component.text(name);
        }
    }

    public void setDisplayNameComponent(Component component) {
        this.displayName = component != null
            ? GsonComponentSerializer.gson().serialize(component)
            : null;
    }
}
