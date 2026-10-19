package dev.rpgprogression.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionExperienceFormatTest {
    @Test void exactAmountsRemainUntilTenThousand(){
        for(boolean ru:new boolean[]{false,true}){assertEquals("0",ProgressionExperienceFormat.format(-1,ru));assertEquals("7",ProgressionExperienceFormat.format(7,ru));assertEquals("9999",ProgressionExperienceFormat.format(9999,ru));}
    }
    @Test void thousandSuffixIsLocalizedAndNeverRoundsUpToAMillion(){
        assertEquals("10 K",ProgressionExperienceFormat.format(10_000,false));assertEquals("10 Т",ProgressionExperienceFormat.format(10_000,true));
        assertEquals("12.3 K",ProgressionExperienceFormat.format(12_345,false));assertEquals("12,3 Т",ProgressionExperienceFormat.format(12_345,true));
        assertEquals("999.9 K",ProgressionExperienceFormat.format(999_999,false));
    }
    @Test void millionsUseTheSameVisibleSuffixInBothLanguages(){
        assertEquals("1 M",ProgressionExperienceFormat.format(1_000_000,false));assertEquals("1 М",ProgressionExperienceFormat.format(1_000_000,true));
        assertEquals("1.2 M",ProgressionExperienceFormat.format(1_299_999,false));assertEquals("9000000000 М",ProgressionExperienceFormat.format(9_000_000_000_000_000L,true));
    }
}
