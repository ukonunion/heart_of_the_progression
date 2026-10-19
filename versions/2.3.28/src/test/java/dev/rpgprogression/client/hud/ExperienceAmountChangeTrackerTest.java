package dev.rpgprogression.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceAmountChangeTrackerTest {
    @Test void initialSnapshotIsSilentAndGainHoldsForFiveSeconds(){
        var tracker=new ExperienceAmountChangeTracker();var player=new Object();
        tracker.update(player,1_000_000);assertFalse(tracker.visible());
        tracker.update(player,1_000_001);assertTrue(tracker.visible());
        for(int i=0;i<99;i++)tracker.update(player,1_000_001);assertTrue(tracker.visible());
        tracker.update(player,1_000_001);assertFalse(tracker.visible());
    }
    @Test void LossAndSingleUnitChangesOfLargeLongAmountsRefreshTheWindow(){
        var tracker=new ExperienceAmountChangeTracker();var player=new Object();long large=9_000_000_000_000_000L;
        tracker.update(player,large);tracker.update(player,large-1);assertTrue(tracker.visible());
        for(int i=0;i<99;i++)tracker.update(player,large-1);
        tracker.update(player,large-2);for(int i=0;i<99;i++)tracker.update(player,large-2);assertTrue(tracker.visible());
        tracker.update(player,large-2);assertFalse(tracker.visible());
        tracker.update(player,0);assertTrue(tracker.visible());
    }
    @Test void reconnectRespawnAndDisableResetTheBaseline(){
        var tracker=new ExperienceAmountChangeTracker();var first=new Object();var second=new Object();
        tracker.update(first,7);tracker.update(first,8);assertTrue(tracker.visible());
        tracker.update(second,8);assertFalse(tracker.visible());
        tracker.update(second,9);tracker.reset();tracker.update(second,9);assertFalse(tracker.visible());
        tracker.update(second,10);tracker.update(null,0);assertFalse(tracker.visible());
        tracker.update(second,100);assertFalse(tracker.visible());
    }
}
