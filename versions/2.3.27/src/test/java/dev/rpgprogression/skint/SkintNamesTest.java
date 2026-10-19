package dev.rpgprogression.skint;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkintNamesTest {
    @Test void acceptsPlayerLabelsAndEmptyReset(){assertEquals("Островок Невера",SkintNames.clean("  Островок Невера  "));assertEquals("",SkintNames.clean(null));assertEquals("",SkintNames.clean(" \n\t"));}
    @Test void removesFormattingAndControlCharacters(){assertEquals("Goldhello",SkintNames.clean("§6Gold\nhello\u202E\u0000"));assertEquals("x",SkintNames.clean("x§"));}
    @Test void limitsUtf16LengthWithoutSplittingSurrogates(){String s=SkintNames.clean("a".repeat(63)+"🌟");assertEquals(63,s.length());assertEquals(64,SkintNames.clean("🌟".repeat(50)).length());}
}
