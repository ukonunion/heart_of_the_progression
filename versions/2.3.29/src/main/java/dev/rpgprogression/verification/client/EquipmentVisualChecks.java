package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.verification.SkintTravelChecks;
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

/** Uses the real tooltip event, not a hand-written rendering of the expected table. */
public final class EquipmentVisualChecks {
    private record Panel(ItemStack stack,List<Component> tooltip){}
    private record Page(String file,Panel left,Panel right){}
    private static final List<Page> pages=new ArrayList<>();private static boolean initialized,done;private static int age,index;
    private static boolean heading(Component c){return c.getContents() instanceof TranslatableContents t&&t.getKey().equals("rpgprogression.requirements.title");}
    private static void check(JsonArray checks,boolean ok,String name){SkintTravelChecks.check(checks,ok,name);if(!ok)throw new IllegalStateException(name);}
    private static void initialize(Minecraft mc,JsonArray checks)throws Exception{
        var d=ClientData.get();var saved=d.save();var panels=new HashMap<String,Panel>();
        try{
            var cases=JsonParser.parseString(Files.readString(Path.of(System.getProperty("rpgprogression.equipmentFixture")))).getAsJsonArray();
            for(var value:cases){var row=value.getAsJsonObject();String id=row.get("id").getAsString();var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));for(var a:RPGAttribute.values())d.set(a,0);
                var lines=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL);var expected=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);row.getAsJsonObject("requirements").entrySet().forEach(e->expected.put(RPGAttribute.valueOf(e.getKey().toUpperCase(Locale.ROOT)),e.getValue().getAsInt()));
                boolean same=expected.isEmpty()?lines.stream().noneMatch(EquipmentVisualChecks::heading):lines.size()>1&&heading(lines.get(1));int i=2;
                for(var e:expected.entrySet()){String text=Component.translatable(e.getKey().key()).getString()+" "+e.getValue();same&=lines.size()>i&&lines.get(i).getString().equals(text);i++;}
                check(checks,same,"native tooltip has the requested unmet requirement rows for "+id);panels.put(id,new Panel(stack,List.copyOf(lines)));
                expected.forEach(d::set);var fulfilled=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL);check(checks,fulfilled.stream().noneMatch(EquipmentVisualChecks::heading),"native tooltip hides all fulfilled requirements at exact thresholds for "+id);
            }
            for(String[] pair:new String[][]{{"epicfight:iron_dagger","epicfight:iron_greatsword"},{"epicfight:iron_tachi","epicfight:iron_longsword"},{"epicfight:diamond_tachi","epicfight:diamond_longsword"},{"epicfight:netherite_tachi","epicfight:netherite_longsword"},{"epicfight:uchigatana","epicfight:iron_spear"},{"minecraft:iron_chestplate","minecraft:diamond_chestplate"},{"minecraft:netherite_chestplate","minecraft:mace"},{"minecraft:iron_axe","minecraft:diamond_axe"},{"minecraft:netherite_axe","minecraft:bow"},{"minecraft:crossbow","epicfight:netherite_dagger"}})
                pages.add(new Page(String.format("equipment-%02d.png",pages.size()+1),panels.get(pair[0]),panels.get(pair[1])));
        }finally{d.load(saved);}
    }
    public static boolean tick(Minecraft mc,JsonArray checks)throws Exception{
        if(done)return true;if(!initialized){initialize(mc,checks);initialized=true;mc.setScreen(new Preview(pages.get(0)));return false;}
        if(++age<8)return false;var folder=mc.gameDirectory.toPath().resolve("screenshots/paths-2.3.22");Files.createDirectories(folder);try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder.resolve(pages.get(index).file));}
        index++;age=0;if(index<pages.size()){mc.setScreen(new Preview(pages.get(index)));return false;}mc.setScreen(null);done=true;return true;
    }
    private static final class Preview extends Screen {
        private final Page page;private Preview(Page page){super(Component.literal("Требования к снаряжению 2.3.21"));this.page=page;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xFF201919);g.drawCenteredString(font,title,width/2,20,0xFFD3BA80);draw(g,page.left,24);draw(g,page.right,width/2+20);}
        private void draw(GuiGraphics g,Panel panel,int x){g.renderItem(panel.stack,x,58);g.renderComponentTooltip(font,panel.tooltip,x,96);}
    }
    private EquipmentVisualChecks(){}
}
