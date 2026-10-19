package dev.rpgprogression.compat;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.info.ServerLimitation;
import dev.rpgprogression.progression.PlayerProgression;
import dev.rpgprogression.skill.SkillNode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** RPG-owned learning graph, retaining save IDs and artwork across ParCool generations. */
public final class ParCoolSkills {
    private static final Map<Class<? extends Action>,String> IDS=new LinkedHashMap<>();
    private static void node(Class<? extends Action> action,String id){IDS.put(action,"parcool:"+id);}
    static {
        node(Crawl.class,"crawl");node(Slide.class,"slide");node(HideInBlock.class,"hide_in_block");node(ChargeJump.class,"charge_jump");
        node(ClingToCliff.class,"hang_on");node(ClimbUp.class,"climb_up");node(ClimbPoles.class,"pole_climb");
        node(WallSlide.class,"slide_down");node(HangDown.class,"hang_down");node(RideZipline.class,"ride_zipline");
        node(FastRun.class,"fast_run");node(FastSwim.class,"fast_swim");node(Vault.class,"vault");node(BreakfallReady.class,"breakfall");
        node(Flipping.class,"trick_jump");node(CatLeap.class,"long_jump");node(Dodge.class,"dodge");
        node(Dive.class,"dive");node(SkyDive.class,"skydive");node(HorizontalWallRun.class,"horizontal_wall_run");
        node(WallJump.class,"wall_jump");node(VerticalWallRun.class,"wall_run");
        IDS.put(JumpFromBar.class,"parcool:hang_down");IDS.put(Roll.class,"parcool:breakfall");IDS.put(Tap.class,"parcool:breakfall");
    }
    public static String id(Class<? extends Action> action){return IDS.get(action);}
    public static List<SkillNode> catalog(ServerPlayer p){return IDS.values().stream().distinct().map(id->new SkillNode(id,1,Component.translatable("rpgprogression.skills.name."+id.replace(':','.')),"parkour",List.of())).toList();}
    public static void refresh(ServerPlayer p,PlayerProgression d){
        // ParCool 4-only Castaway cannot run on 3.x: refund its paid point exactly once.
        if(d.unlockedSkills.remove("parcool:castaway")){d.skillPoints++;d.dirty=true;}
        if(!d.parcoolSkillsImported){d.parcoolSkillsImported=true;d.dirty=true;}
    }
    public static void apply(ServerPlayer p,PlayerProgression d){refresh(p,d);ParCoolBridge.mirror(p);}
    public static boolean canUnlock(ServerPlayer p,String id){return IDS.containsValue(id)&&IDS.entrySet().stream().filter(e->e.getValue().equals(id)).allMatch(e->ServerLimitation.get(p).isPermitted(e.getKey()));}
    private ParCoolSkills(){}
}
