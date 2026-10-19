package dev.rpgprogression.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LevelXPFormulaTest {
    private double value(String formula){return LevelXPFormula.evaluate(formula,4,500,1.2);}
    @Test void variablesAndTypicalServerFormulas(){
        assertEquals(950,value("base + (level - 1) * 150"));
        assertEquals(8000,value("base * level ^ 2"));
        assertEquals(864,value("base * multiplier ^ (level - 1)"),1e-9);
        assertEquals(500,value("base"));
        assertEquals(500,LevelXPFormula.evaluate("base * level",0,500,1));
    }
    @Test void arithmeticPrecedenceAndRightAssociativePower(){
        assertEquals(14,value("2 + 3 * 4"));assertEquals(512,value("2 ^ 3 ^ 2"));
        assertEquals(-4,value("-2 ^ 2"));assertEquals(.25,value("2 ^ -2"));
        assertEquals(4,value("(-2) ^ 2"));assertEquals(3,value("11 % 4"));
        assertEquals(100.005,value("1e2 + 5E-3"));
    }
    @Test void supportedFunctionsAndConstants(){
        assertEquals(10,value("clamp(pow(2, 4), 1, 10)"));
        assertEquals(5,value("min(9, max(3, abs(-5)))"));
        assertEquals(8,value("sqrt(9) + floor(1.9) + ceil(1.1) + round(1.5)"));
        assertEquals(3,value("log(exp(3))"),1e-9);
        assertEquals(Math.PI+Math.E,value("pi + e"));
    }
    @Test void unknownNamesScriptsAndWrongArityAreRejected(){
        for(String formula:new String[]{"", " ", "level *", "x + 1", "Math.pow(level,2)", "System.exit(0)", "level; base", "base = 0", "1,2", "min(1)", "pow(1,2,3)", "max()", "sqrt(1,2)", "(1+2", "1e309", "1..2", "Infinity", "NaN"})
            assertFalse(LevelXPFormula.valid(formula),formula);
        assertFalse(LevelXPFormula.valid(null));
    }
    @Test void parsingAndEvaluationHaveBoundedComplexity(){
        assertFalse(LevelXPFormula.valid("(".repeat(40)+"1"+")".repeat(40)));
        assertFalse(LevelXPFormula.valid("1+".repeat(140)+"1"));
        assertFalse(LevelXPFormula.valid("1".repeat(1025)));
        assertTrue(LevelXPFormula.valid("max(1, base * level ^ 2)"));
    }
    @Test void costsRoundClampAndRecoverFromUndefinedExpressions(){
        var curve=ProgressionMath.Curve.CUSTOM;
        assertEquals(501,ProgressionMath.required(1,500.5,1,curve,"base"));
        assertEquals(1,ProgressionMath.required(1,500,1,curve,"-10"));
        assertEquals(1,ProgressionMath.required(1,500,1,curve,"0"));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.required(1000000,500,1,curve,"base * 10 ^ level"));
        assertEquals(ProgressionMath.required(50,500),ProgressionMath.required(50,500,1,curve,"sqrt(-1)"));
        assertEquals(ProgressionMath.required(50,500),ProgressionMath.required(50,500,1,curve,"invalid"));
    }
    @Test void cacheTracksChangedFormulaAndFlatExpressions(){
        assertEquals(500,value("base"));assertEquals(2000,value("base * level"));assertEquals(500,value("base"));
        assertTrue(LevelXPFormula.levelIndependent("base * multiplier + 50"));
        assertFalse(LevelXPFormula.levelIndependent("base + level"));
        assertFalse(LevelXPFormula.levelIndependent("invalid"));
    }
    @Test void existingCurvesIgnoreTheCustomFormula(){
        assertEquals(41000,ProgressionMath.required(100,500,1.1,ProgressionMath.Curve.STAGED,"0"));
        assertEquals(500,ProgressionMath.required(100,500,1,ProgressionMath.Curve.EXPONENTIAL,"0"));
        assertEquals(8000,ProgressionMath.required(4,500,1,ProgressionMath.Curve.CUSTOM));
    }
}
