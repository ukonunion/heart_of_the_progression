package dev.rpgprogression.client;

import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Local snapshots only: never downloads images or asks a server for files. */
public final class SkintPreviews {
    private static final Map<UUID,ResourceLocation> textures=new LinkedHashMap<>(32,.75f,true);
    private static final Set<UUID> absent=new HashSet<>();
    private static UUID pending;private static int frames;
    private static Path path(UUID id){return Minecraft.getInstance().gameDirectory.toPath().resolve("rpgprogression/skint-previews/"+id+".png");}
    public static void request(UUID id){pending=id;frames=0;}
    public static void capture(){
        if(pending==null||!SkintCamera.active()||!ClientSkintData.allowed())return;
        if(!SkintCamera.settled()||++frames<3)return;var id=pending;pending=null;
        try(var screenshot=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());var image=new NativeImage(640,360,false)){
            // Center-crop to 16:9 before resizing, without stretching a different window aspect.
            int w=screenshot.getWidth(),h=screenshot.getHeight();int cropW=Math.min(w,h*16/9),cropH=Math.min(h,w*9/16);
            screenshot.resizeSubRectTo((w-cropW)/2,(h-cropH)/2,cropW,cropH,image);
            Files.createDirectories(path(id).getParent());image.writeToFile(path(id));
            release(id);absent.remove(id);
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().warn("Could not save skint preview {}",id,error);}
    }
    public static ResourceLocation get(UUID id){
        if(textures.containsKey(id))return textures.get(id);if(absent.contains(id))return null;
        if(!Files.isRegularFile(path(id))){absent.add(id);return null;}
        try(var input=Files.newInputStream(path(id))){
            var image=NativeImage.read(input);
            if(image.getWidth()!=640||image.getHeight()!=360){image.close();absent.add(id);return null;}
            var location=ResourceLocation.fromNamespaceAndPath("rpgprogression","skint_preview/"+id);
            Minecraft.getInstance().getTextureManager().register(location,new DynamicTexture(image));textures.put(id,location);
            if(textures.size()>32)release(textures.keySet().iterator().next());return location;
        }catch(Exception error){absent.add(id);com.mojang.logging.LogUtils.getLogger().warn("Could not load skint preview {}",id,error);return null;}
    }
    private static void release(UUID id){var location=textures.remove(id);if(location!=null)Minecraft.getInstance().getTextureManager().release(location);}
    public static void cancel(){pending=null;}
    public static void clear(){cancel();for(var id:List.copyOf(textures.keySet()))release(id);absent.clear();}
    private SkintPreviews(){}
}
