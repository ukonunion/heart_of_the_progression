package dev.rpgprogression.compat;

import com.alrex.parcool.api.stamina.AbstractLocalStamina;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.world.entity.player.Player;

/** Client prediction over the synchronized RPG capability, with no second regen loop. */
public final class RPGLocalStamina extends AbstractLocalStamina {
    private double previous=-1,previousMax=-1;
    private boolean previousExhausted;
    public RPGLocalStamina(Player owner){super(owner);}
    @Override public double max(){return ProgressionManager.get(owner).maxStamina();}
    @Override public double value(){return ProgressionManager.get(owner).stamina;}
    @Override public boolean isInfinite(){return ParCoolBridge.infinite(owner);}
    @Override public boolean isExhausted(){return !isInfinite()&&ProgressionManager.get(owner).exhausted;}
    @Override public boolean imposePenalty(){return false;}
    @Override public boolean showHud(){return false;}
    @Override public void consume(double value){
        if(!isInfinite())StaminaManager.spend(owner,ProgressionManager.get(owner),value*RPGConfig.PARCOOL_COST.get());
    }
    // ParCool must not restore or overwrite server-owned RPG stamina.
    @Override public void recover(double value){}
    @Override public void setValue(double value){}
    @Override public void tick(){
        if(value()!=previous||max()!=previousMax||isExhausted()!=previousExhausted){
            previous=value();previousMax=max();previousExhausted=isExhausted();setDirty();
        }
    }
}
