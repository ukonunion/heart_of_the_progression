package dev.rpgprogression.compat;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import java.util.*;

/** Query mod metadata before any optional target class is loaded. */
public final class OptionalMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        if(mixin.contains(".epicparcool."))return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("epicparcool")&&m.getVersion().toString().equals("21.0.0"));
        if(mixin.contains(".irons."))return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("irons_spellbooks")&&m.getVersion().toString().startsWith("1.21.1-3.16."));
        if(mixin.contains(".epicfight."))return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("epicfight")&&m.getVersion().toString().startsWith("21.16.4"));
        if(!mixin.contains(".parcool."))return true;
        return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(m->m.getModId().equals("parcool")&&m.getVersion().toString().startsWith("3.4.3."));
    }
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){
        // ParCool's client-only helpers must be stripped before the combined mixins recompute frames.
        // This matches NeoForge's later dist cleaning and prevents client attachment loading on servers.
        if(target.equals("com.alrex.parcool.common.action.ActionProcessor")&&net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.DEDICATED_SERVER){
            node.methods.removeIf(m->clientOnly(m.visibleAnnotations)||clientOnly(m.invisibleAnnotations));
            // These two native helpers lack @OnlyIn, although their callers only use them on clients.
            // Keep the method signatures for those guarded calls, without loading client classes.
            for(var method:node.methods)if(method.name.equals("consumeStamina")||method.name.equals("onTick$doPostProcessInClient")){
                method.instructions.clear();
                method.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.RETURN));
                method.tryCatchBlocks.clear();
                if(method.localVariables!=null)method.localVariables.clear();
                method.maxStack=0;
            }
        }
    }
    private static boolean clientOnly(java.util.List<org.objectweb.asm.tree.AnnotationNode> annotations){
        if(annotations==null)return false;
        for(var a:annotations)if(a.desc.equals("Lnet/neoforged/api/distmarker/OnlyIn;")&&a.values!=null)
            for(Object value:a.values)if(value instanceof String[] enumeration&&enumeration.length==2&&enumeration[1].equals("CLIENT"))return true;
        return false;
    }
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
