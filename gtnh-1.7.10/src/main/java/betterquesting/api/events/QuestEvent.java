package betterquesting.api.events;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.Event;

public class QuestEvent extends Event {

    public enum Type {
        COMPLETED,
        UPDATED,
        RESET
    }

    public Type getType() {
        return null;
    }

    public UUID getPlayerID() {
        return null;
    }

    public Set<UUID> getQuestIDs() {
        return Collections.emptySet();
    }
}
