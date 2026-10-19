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
    private static final Map<String,List<String>> PARENTS=new LinkedHashMap<>();
    private static void node(Class<? extends Action> action,String id,String parent){IDS.put(action,"parcool:"+id);PARENTS.put("parcool:"+id,parent==null?List.of():List.of("parcool:"+parent));}
    static {
        node(Crawl.class,"crawl",null);node(Slide.class,"slide","crawl");node(HideInBlock.class,"hide_in_block","crawl");node(ChargeJump.class,"charge_jump","crawl");
        node(ClingToCliff.class,"hang_on",null);node(ClimbUp.class,"climb_up","hang_on");node(ClimbPoles.class,"pole_climb","climb_up");
        node(WallSlide.class,"slide_down","hang_on");node(HangDown.class,"hang_down","hang_on");node(RideZipline.class,"ride_zipline","hang_on");
        node(FastRun.class,"fast_run",null);node(FastSwim.class,"fast_swim","fast_run");node(Vault.class,"vault","fast_run");node(BreakfallReady.class,"breakfall","vault");
        node(Flipping.class,"trick_jump","fast_run");node(CatLeap.class,"long_jump","trick_jump");node(Dodge.class,"dodge","trick_jump");
        node(Dive.class,"dive","fast_run");node(SkyDive.class,"skydive","dive");node(HorizontalWallRun.class,"horizontal_wall_run","fast_run");
        node(WallJump.class,"wall_jump","fast_run");node(VerticalWallRun.class,"wall_run","wall_jump");
        IDS.put(JumpFromBar.class,"parcool:hang_down");IDS.put(Roll.class,"parcool:breakfall");IDS.put(Tap.class,"parcool:breakfall");
    }
    public static String id(Class<? extends Action> action){return IDS.get(action);}
    public static List<SkillNode> catalog(ServerPlayer p){return PARENTS.entrySet().stream().map(e->new SkillNode(e.getKey(),1,Component.translatable("rpgprogression.skills.name."+e.getKey().replace(':','.')),"parkour",e.getValue())).toList();}
    public static void refresh(ServerPlayer p,PlayerProgression d){
        // ParCool 4-only Castaway cannot run on 3.x: refund its paid point exactly once.
        if(d.unlockedSkills.remove("parcool:castaway")){d.skillPoints++;d.dirty=true;}
        if(!d.parcoolSkillsImported){d.parcoolSkillsImported=true;d.dirty=true;}
    }
    public static void apply(ServerPlayer p,PlayerProgression d){refresh(p,d);ParCoolBridge.mirror(p);}
    public static boolean canUnlock(ServerPlayer p,String id){return PARENTS.containsKey(id)&&IDS.entrySet().stream().filter(e->e.getValue().equals(id)).allMatch(e->ServerLimitation.get(p).isPermitted(e.getKey()));}
    private ParCoolSkills(){}
}
