package cafe.zach.greglink.bridge.events.server;

import java.util.Set;
import java.util.UUID;

import betterquesting.api.events.QuestEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BetterQuestingQuestListener {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onQuestEvent(QuestEvent event) {
        if (event == null || event.getType() != QuestEvent.Type.COMPLETED) return;
        if (!FMLCommonHandler.instance()
            .getEffectiveSide()
            .isServer()) return;

        Set<UUID> questIds = event.getQuestIDs();
        if (questIds == null || questIds.isEmpty()) return;

        UUID playerId = event.getPlayerID();
        if (playerId == null) return;

        BetterQuestingQuestHandler.onQuestsCompleted(playerId, questIds);
    }
}
