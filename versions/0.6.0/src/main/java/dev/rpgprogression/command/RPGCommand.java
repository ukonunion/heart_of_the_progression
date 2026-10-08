package dev.rpgprogression.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import static net.minecraft.commands.Commands.*;

public final class RPGCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root=literal("rpg").requires(s->s.hasPermission(2));
        root.then(literal("level")
            .then(literal("get").executes(c->{var d=ProgressionManager.get(c.getSource().getPlayerOrException());c.getSource().sendSuccess(()->Component.translatable("rpgprogression.command.status",d.level,d.xp,d.points),false);return d.level;}))
            .then(literal("set").then(argument("value",IntegerArgumentType.integer(1,1000)).executes(c->change(c,(p,d)->{d.level=Math.min(IntegerArgumentType.getInteger(c,"value"),RPGConfig.MAX_LEVEL.get());d.xp=0;})))));
        root.then(literal("xp")
            .then(literal("add").then(argument("amount",LongArgumentType.longArg(0,ProgressionMath.XP_LIMIT)).executes(c->change(c,(p,d)->ProgressionManager.addXPAdministrative(p,LongArgumentType.getLong(c,"amount"))))))
            .then(literal("set").then(argument("amount",LongArgumentType.longArg(0,ProgressionMath.XP_LIMIT)).executes(c->change(c,(p,d)->{d.xp=0;ProgressionManager.addXPAdministrative(p,LongArgumentType.getLong(c,"amount"));})))));
        root.then(literal("points").then(literal("add").then(argument("amount",IntegerArgumentType.integer(0)).executes(c->change(c,(p,d)->d.points=(int)Math.min(Integer.MAX_VALUE,(long)d.points+IntegerArgumentType.getInteger(c,"amount")))))));
        var stat=literal("stat");var set=literal("set");
        for(RPGAttribute attribute:RPGAttribute.values())set.then(literal(attribute.id()).then(argument("value",IntegerArgumentType.integer(0,10000)).executes(c->change(c,(p,d)->d.set(attribute,Math.min(RPGConfig.MAX_ATTRIBUTE.get(),IntegerArgumentType.getInteger(c,"value")))))));
        root.then(stat.then(set));
        root.then(literal("stamina").then(literal("set").then(argument("value",DoubleArgumentType.doubleArg(0,10000000)).executes(c->change(c,(p,d)->d.stamina=Math.min(d.maxStamina(),DoubleArgumentType.getDouble(c,"value")))))));
        root.then(literal("spark").then(literal("set").then(argument("value",DoubleArgumentType.doubleArg(0,1000000000)).executes(c->change(c,(p,d)->dev.rpgprogression.resource.SparkManager.set(p,d,DoubleArgumentType.getDouble(c,"value")))))));
        root.then(literal("mana").then(literal("set").then(argument("value",DoubleArgumentType.doubleArg(0,1000000000)).executes(c->change(c,(p,d)->d.mana=Math.min(d.manaCapacity,DoubleArgumentType.getDouble(c,"value")))))));
        root.then(literal("reset").executes(c->change(c,(p,d)->{d.load(new PlayerProgression().save());d.initialized=false;d.initialize();d.exhausted=false;d.regenCooldown=0;})));
        dispatcher.register(root);
    }
    private static int change(CommandContext<CommandSourceStack> c,Mutation mutation) throws CommandSyntaxException {
        ServerPlayer p=c.getSource().getPlayerOrException();PlayerProgression d=ProgressionManager.get(p);int old=d.get(RPGAttribute.INTELLIGENCE);
        mutation.apply(p,d);d.dirty=true;ProgressionManager.refresh(p,old,true);
        c.getSource().sendSuccess(()->Component.translatable("rpgprogression.command.updated"),true);return 1;
    }
    private interface Mutation {void apply(ServerPlayer p,PlayerProgression d);}
    private RPGCommand() {}
}
