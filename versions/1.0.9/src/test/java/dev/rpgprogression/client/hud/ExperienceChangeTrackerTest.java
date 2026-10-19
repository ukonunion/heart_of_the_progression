package dev.rpgprogression.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceChangeTrackerTest {
    @Test void loginDoesNotFlashButGainsAndSpendingShowAndRefresh(){
        var tracker=new ExperienceChangeTracker();Object session=new Object();
        tracker.update(session,125,8,.5f);assertFalse(tracker.visible());
        tracker.update(session,126,8,.6f);assertTrue(tracker.visible());
        for(int i=0;i<99;i++)tracker.update(session,126,8,.6f);assertTrue(tracker.visible());
        tracker.update(session,126,8,.6f);assertFalse(tracker.visible());
        tracker.update(session,126,5,.6f);assertTrue(tracker.visible()); // Level spending can leave total XP unchanged.
        for(int i=0;i<99;i++)tracker.update(session,126,5,.6f);
        tracker.update(session,126,5,.4f); // A points-only command refreshes the window as well.
        for(int i=0;i<99;i++)tracker.update(session,126,5,.4f);assertTrue(tracker.visible());
        tracker.update(session,126,5,.4f);assertFalse(tracker.visible());
        tracker.update(session,0,0,0);assertTrue(tracker.visible()); // Losing XP on death.
    }
    @Test void reconnectAndResetForgetThePreviousPlayersWindow(){
        var tracker=new ExperienceChangeTracker();Object first=new Object(),second=new Object();
        tracker.update(first,5,1,.2f);tracker.update(first,6,1,.3f);assertTrue(tracker.visible());
        tracker.update(second,40,4,.8f);assertFalse(tracker.visible());
        tracker.update(second,41,4,.9f);assertTrue(tracker.visible());
        tracker.update(null,0,0,0);assertFalse(tracker.visible());
        tracker.update(second,41,4,.9f);assertFalse(tracker.visible());
        tracker.update(second,42,4,.95f);tracker.reset();assertFalse(tracker.visible());
        tracker.update(second,42,4,.95f);assertFalse(tracker.visible());
    }
}
