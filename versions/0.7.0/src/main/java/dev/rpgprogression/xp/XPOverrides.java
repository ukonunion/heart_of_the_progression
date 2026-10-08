package dev.rpgprogression.xp;

import net.minecraft.core.*;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public final class XPOverrides {
    private static final Map<Object,List<? extends String>> snapshots=new IdentityHashMap<>();
    private static final Map<Object,List<XPRule>> parsed=new IdentityHashMap<>();
    private static synchronized List<XPRule> rules(ForgeConfigSpec.ConfigValue<List<? extends String>> config){
        var source=config.get();if(!source.equals(snapshots.get(config))){snapshots.put(config,List.copyOf(source));parsed.put(config,source.stream().map(XPRule::parse).toList());}
        return parsed.get(config);
    }
    public static <T> double amount(ForgeConfigSpec.ConfigValue<List<? extends String>> config,Holder<T> holder,ResourceKey<? extends Registry<T>> registry,double fallback){
        String id=holder.unwrapKey().map(k->k.location().toString()).orElse("");
        Double exact=null,tag=null;
        for(var rule:rules(config)){
            if(!rule.tag()&&rule.id().equals(id))exact=rule.amount();
            else if(rule.tag()&&holder.is(TagKey.create(registry,new ResourceLocation(rule.id()))))tag=tag==null?rule.amount():Math.max(tag,rule.amount());
        }
        return exact!=null?exact:tag!=null?tag:fallback;
    }
    public static long reward(double amount){return Math.round(Math.max(0,Math.min(dev.rpgprogression.progression.ProgressionMath.XP_LIMIT,amount)));}
    private XPOverrides(){}
}
