package com.cavetale.election;

import static com.cavetale.core.util.CamelCase.toCamelCase;

public enum ElectionType {
    PICK_ONE, // SQLBalloto
    UP_DOWN_VOTE; // SQLVote

    public String getDisplayName() {
        return toCamelCase(" ", this);
    }
}
