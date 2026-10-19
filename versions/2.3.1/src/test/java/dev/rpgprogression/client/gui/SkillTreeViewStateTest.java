package dev.rpgprogression.client.gui;

import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SkillTreeViewStateTest {
    @TempDir Path directory;
    @Test void missingPreferencesUseTheDefaultWithoutCreatingAFile()throws IOException {
        Path file=directory.resolve("config/view.json");
        assertTrue(SkillTreeViewState.read(file).isEmpty());assertFalse(Files.exists(file.getParent()));
    }
    @Test void exactCoordinatesSurviveReplacementAndLeaveNoTemporaryFiles()throws IOException {
        Path file=directory.resolve("config/view.json");
        new SkillTreeViewState(0,0,1.25f).write(file);
        var changed=new SkillTreeViewState(174.99130434782612,428.52173913043475,1.4375f);changed.write(file);
        assertEquals(changed,SkillTreeViewState.read(file).orElseThrow());
        try(var files=Files.list(file.getParent())){assertEquals(1,files.count());}
    }
    @Test void corruptOrUnsupportedPreferencesCannotInstallAnInvalidCamera()throws IOException {
        Path file=directory.resolve("view.json");
        for(String text:new String[]{"{", "[]", "{}", "{\"version\":2,\"x\":0,\"y\":0,\"zoom\":1}",
                "{\"version\":1,\"x\":\"wrong\",\"y\":0,\"zoom\":1}", "{\"version\":1,\"x\":1e999,\"y\":0,\"zoom\":1}",
                "{\"version\":1,\"x\":0,\"y\":0,\"zoom\":0}", "{\"version\":1,\"x\":0,\"y\":0,\"zoom\":2}"}){
            Files.writeString(file,text);assertTrue(SkillTreeViewState.read(file).isEmpty(),text);assertEquals(text,Files.readString(file));
        }
    }
    @Test void invalidCoordinatesAndZoomAreRejectedBeforeWriting(){
        assertThrows(IllegalArgumentException.class,()->new SkillTreeViewState(Double.NaN,0,1));
        assertThrows(IllegalArgumentException.class,()->new SkillTreeViewState(0,Double.POSITIVE_INFINITY,1));
        assertThrows(IllegalArgumentException.class,()->new SkillTreeViewState(0,0,Float.NaN));
        assertThrows(IllegalArgumentException.class,()->new SkillTreeViewState(0,0,.1f));
        assertThrows(IllegalArgumentException.class,()->new SkillTreeViewState(0,0,2));
    }
    @Test void failedWritesDoNotReplaceAnUnrelatedParentFile()throws IOException {
        Path blocked=directory.resolve("blocked");Files.writeString(blocked,"keep");
        assertThrows(IOException.class,()->new SkillTreeViewState(0,0,1).write(blocked.resolve("view.json")));
        assertEquals("keep",Files.readString(blocked));
    }
}
