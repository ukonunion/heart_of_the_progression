package dev.rpgprogression.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;

/** Local view preferences; never contains player progression or server data. */
public record SkillTreeViewState(double x,double y,float zoom) {
    public SkillTreeViewState {
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Float.isFinite(zoom)||zoom<.2f||zoom>1.75f)
            throw new IllegalArgumentException("Invalid skill-tree view");
    }
    public static Optional<SkillTreeViewState> read(Path file)throws IOException {
        if(!Files.exists(file))return Optional.empty();
        try {
            var data=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonObject();
            for(String key:new String[]{"version","x","y","zoom"})
                if(!data.has(key)||!data.get(key).isJsonPrimitive()||!data.getAsJsonPrimitive(key).isNumber())return Optional.empty();
            if(data.get("version").getAsDouble()!=1)return Optional.empty();
            return Optional.of(new SkillTreeViewState(data.get("x").getAsDouble(),data.get("y").getAsDouble(),data.get("zoom").getAsFloat()));
        }catch(RuntimeException malformed){return Optional.empty();}
    }
    public void write(Path file)throws IOException {
        Path target=file.toAbsolutePath();Files.createDirectories(target.getParent());
        var data=new JsonObject();data.addProperty("version",1);data.addProperty("x",x);data.addProperty("y",y);data.addProperty("zoom",zoom);
        Path temporary=Files.createTempFile(target.getParent(),target.getFileName()+".",".tmp");
        try {
            Files.writeString(temporary,data.toString()+"\n",StandardCharsets.UTF_8);
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
}
