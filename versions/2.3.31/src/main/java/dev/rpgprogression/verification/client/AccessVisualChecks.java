package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.requirements.ItemRequirements;
import dev.rpgprogression.verification.AccessRulesChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;

public final class AccessVisualChecks {
    public record Panel(String label,ItemStack stack,List<Component> lines){}
    private record Page(String file,Panel left,Panel right){}
    private static final List<Page> pages=new ArrayList<>();private static boolean done,initialized;private static int index,age;
    private static boolean heading(Component c){return c.getContents() instanceof TranslatableContents t&&t.getKey().equals("rpgprogression.requirements.title");}
    private static Panel panel(Minecraft mc,JsonArray checks,ItemStack stack,String label,int intelligence,int potential,Map<RPGAttribute,Integer> expected){
        var d=ClientData.get();for(var a:RPGAttribute.values())d.set(a,100);d.set(RPGAttribute.INTELLIGENCE,intelligence);d.set(RPGAttribute.SPARK_POTENTIAL,potential);
        var lines=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL);boolean same=expected.isEmpty()?lines.stream().noneMatch(AccessVisualChecks::heading):lines.size()>1&&heading(lines.get(1));int i=2;
        var sorted=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);sorted.putAll(expected);for(var e:sorted.entrySet()){same&=lines.size()>i&&lines.get(i).getString().equals(Component.translatable(e.getKey().key()).getString()+" "+e.getValue());i++;}
        AccessRulesChecks.check(checks,same,"native access tooltip has exactly the requested rows for "+BuiltInRegistries.ITEM.getKey(stack.getItem())+" ("+label+")");return new Panel(label,stack,List.copyOf(lines));
    }
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static void initialize(Minecraft mc,JsonArray checks)throws Exception{
        var d=ClientData.get();var saved=d.save();var low=new HashMap<String,Panel>();var high=new HashMap<String,Panel>();try{
            var data=JsonParser.parseString(Files.readString(Path.of(System.getProperty("rpgprogression.redstoneFixture")))).getAsJsonObject();for(var value:data.getAsJsonArray("required")){String id=value.getAsString();low.put(id,panel(mc,checks,item(id),"Интеллект 14",14,100,Map.of(RPGAttribute.INTELLIGENCE,15)));high.put(id,panel(mc,checks,item(id),"Интеллект 15",15,100,Map.of()));}
            pages.add(new Page("access-01-redstone.png",low.get("minecraft:lever"),high.get("minecraft:lever")));pages.add(new Page("access-02-dispenser-dropper.png",low.get("minecraft:dispenser"),low.get("minecraft:dropper")));
            var exempt=new HashMap<String,Panel>();for(var value:data.getAsJsonArray("exempt")){String id=value.getAsString();exempt.put(id,panel(mc,checks,item(id),"Интеллект 0: исключение",0,100,Map.of()));}pages.add(new Page("access-03-exceptions.png",exempt.get("minecraft:big_dripleaf"),exempt.get("minecraft:chest")));
            pages.add(new Page("access-04-ender-chest.png",panel(mc,checks,new ItemStack(Items.ENDER_CHEST),"Потенциал искры 19",0,19,Map.of(RPGAttribute.SPARK_POTENTIAL,20)),panel(mc,checks,new ItemStack(Items.ENDER_CHEST),"Потенциал искры 20",0,20,Map.of())));
            var artifact=BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(dev.rpgprogression.compat.RelicsRequirements::artifact).findFirst();if(artifact.isPresent())pages.add(new Page("access-05-relics.png",panel(mc,checks,artifact.get(),"Интеллект 14",14,100,Map.of(RPGAttribute.INTELLIGENCE,15)),panel(mc,checks,artifact.get(),"Интеллект 15",15,100,Map.of())));
            if(dev.rpgprogression.compat.IronsCompat.present())AccessIronsVisuals.add(mc,checks,(a,b,file)->pages.add(new Page(file,a,b)));
        }finally{d.load(saved);}
    }
    public static boolean tick(Minecraft mc,JsonArray checks)throws Exception{
        if(done)return true;if(!initialized){initialize(mc,checks);initialized=true;mc.setScreen(new Preview(pages.get(0)));return false;}if(++age<8)return false;
        var folder=mc.gameDirectory.toPath().resolve("screenshots/paths-2.3.22");Files.createDirectories(folder);try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder.resolve(pages.get(index).file));}index++;age=0;if(index<pages.size()){mc.setScreen(new Preview(pages.get(index)));return false;}mc.setScreen(null);done=true;return true;
    }
    private static final class Preview extends Screen {
        private final Page page;private Preview(Page page){super(Component.literal("Требования 2.3.22"));this.page=page;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xFF201919);g.drawCenteredString(font,title,width/2,16,0xFFD3BA80);draw(g,page.left,24);draw(g,page.right,width/2+20);}
        private void draw(GuiGraphics g,Panel p,int x){g.drawString(font,p.label,x,42,0xFFE8E2D4,false);g.renderItem(p.stack,x,62);g.renderComponentTooltip(font,p.lines,x,96);}
    }
    private AccessVisualChecks(){}
}
