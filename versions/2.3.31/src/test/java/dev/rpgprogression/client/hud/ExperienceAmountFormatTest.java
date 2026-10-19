package dev.rpgprogression.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceAmountFormatTest {
    private static int width(String text){return text.length()*6;}
    @Test void fullIntegersRemainWhileTheyFitIncludingTheOldThresholds(){
        for(boolean ru:new boolean[]{false,true})for(long value:new long[]{0,7,9999,10000,1000000,99999999})
            assertEquals(Long.toString(value),ExperienceAmountFormat.format(value,ru,50,ExperienceAmountFormatTest::width));
    }
    @Test void overflowUsesLocalizedSuffixesWithoutRoundingUp(){
        assertEquals("123.4 M",ExperienceAmountFormat.format(123456789,false,50,ExperienceAmountFormatTest::width));
        assertEquals("123,4 М",ExperienceAmountFormat.format(123456789,true,50,ExperienceAmountFormatTest::width));
        assertEquals("999.9 M",ExperienceAmountFormat.format(999999999,false,50,ExperienceAmountFormatTest::width));
        assertEquals("1.2 B",ExperienceAmountFormat.format(1234567890,false,50,ExperienceAmountFormatTest::width));
    }
    @Test void fontMetricsAndAvailableSpaceDetermineTheSwitch(){
        assertEquals("123456789",ExperienceAmountFormat.format(123456789,false,54,ExperienceAmountFormatTest::width));
        assertEquals("123456789",ExperienceAmountFormat.format(123456789,false,50,String::length));
        assertEquals("10 K",ExperienceAmountFormat.format(10000,false,24,ExperienceAmountFormatTest::width));
        assertEquals("1 Млрд",ExperienceAmountFormat.format(1234567890,true,36,ExperienceAmountFormatTest::width));
    }
    @Test void veryLargeLongValuesStayShortAndDoNotOverflowDuringFormatting(){
        assertEquals("1.2 Q",ExperienceAmountFormat.format(1234567890123456L,false,50,ExperienceAmountFormatTest::width));
        assertEquals("1,2 Квдр",ExperienceAmountFormat.format(1234567890123456L,true,50,ExperienceAmountFormatTest::width));
        assertEquals("9223.3 Q",ExperienceAmountFormat.format(Long.MAX_VALUE,false,50,ExperienceAmountFormatTest::width));
        assertEquals("0",ExperienceAmountFormat.format(-1,false,50,ExperienceAmountFormatTest::width));
    }
}
