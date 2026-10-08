package dev.rpgprogression.compat;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import java.util.*;

/** Query mod metadata before any optional target class is loaded. */
public final class OptionalMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        if(mixin.contains(".irons."))return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("irons_spellbooks")&&m.getVersion().toString().startsWith("1.20.1-3.16."));
        if(mixin.contains(".epicfight."))return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("epicfight")&&m.getVersion().toString().startsWith("20.14."));
        if(!mixin.contains(".parcool."))return true;
        return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("parcool")&&m.getVersion().toString().startsWith("4."));
    }
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
