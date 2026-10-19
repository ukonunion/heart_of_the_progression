package dev.rpgprogression.verification.client;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.verification.SkintTravelChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;

/** Render actual Minecraft item tooltips at both sides of each attribute threshold. */
public final class RequirementsVisualChecks {
    private record Panel(String label,List<Component> tooltip,ItemStack stack){}
    private record Page(String file,Panel left,Panel right){}
    private static final List<Page> pages=new ArrayList<>();
    private static int page,age;
    private static boolean initialized,done;
    private static void check(JsonArray checks,boolean passed,String name){SkintTravelChecks.check(checks,passed,name);if(!passed)throw new IllegalStateException(name);}
    private static boolean requirement(Component c){return c.getContents() instanceof TranslatableContents t&&t.getKey().equals("rpgprogression.requirements.title");}
    private static Panel panel(Minecraft mc,JsonArray checks,Item item,int endurance,int intelligence,boolean needs,String label){
        var d=ClientData.get();d.set(RPGAttribute.ENDURANCE,endurance);d.set(RPGAttribute.INTELLIGENCE,intelligence);
        var stack=new ItemStack(item);var tooltip=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL);
        check(checks,tooltip.stream().anyMatch(RequirementsVisualChecks::requirement)==needs,label+": native tooltip shows only the expected unmet requirement");
        if(needs)check(checks,tooltip.size()>1&&requirement(tooltip.get(1)),label+": the requirement appears immediately beneath the item name");
        return new Panel(label,List.copyOf(tooltip),stack);
    }
    private static void initialize(Minecraft mc,JsonArray checks){
        var d=ClientData.get();var saved=d.save();
        try{
            pages.add(new Page("01-iron-threshold.png",panel(mc,checks,Items.IRON_PICKAXE,14,10,true,"Выносливость 14"),panel(mc,checks,Items.IRON_PICKAXE,15,10,false,"Выносливость 15")));
            pages.add(new Page("02-diamond-threshold.png",panel(mc,checks,Items.DIAMOND_SWORD,19,10,true,"Выносливость 19"),panel(mc,checks,Items.DIAMOND_SWORD,20,10,false,"Выносливость 20")));
            pages.add(new Page("03-netherite-threshold.png",panel(mc,checks,Items.NETHERITE_AXE,24,10,true,"Выносливость 24"),panel(mc,checks,Items.NETHERITE_AXE,25,10,false,"Выносливость 25")));
            pages.add(new Page("04-unrestricted-tools.png",panel(mc,checks,Items.WOODEN_SWORD,0,0,false,"Деревянный меч: без требований"),panel(mc,checks,Items.STONE_PICKAXE,0,0,false,"Каменная кирка: без требований")));
            pages.add(new Page("05-brewing-threshold.png",panel(mc,checks,Items.BREWING_STAND,10,14,true,"Интеллект 14"),panel(mc,checks,Items.BREWING_STAND,10,15,false,"Интеллект 15")));
            pages.add(new Page("06-enchanting-threshold.png",panel(mc,checks,Items.ENCHANTING_TABLE,10,14,true,"Интеллект 14"),panel(mc,checks,Items.ENCHANTING_TABLE,10,15,false,"Интеллект 15")));
            var original=List.copyOf(RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).get());
            try{
                RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("minecraft:diamond_sword=15"));d.set(RPGAttribute.STRENGTH,14);
                var partial=panel(mc,checks,Items.DIAMOND_SWORD,20,15,true,"Выносливость 20, Сила 14");
                var text=partial.tooltip.stream().filter(RequirementsVisualChecks::requirement).findFirst().orElseThrow().getString();
                check(checks,text.contains("Сила")&&!text.contains("Выносливость"),"meeting Endurance hides it while the Strength requirement remains visible");
                d.set(RPGAttribute.STRENGTH,15);var complete=panel(mc,checks,Items.DIAMOND_SWORD,20,15,false,"Выносливость 20, Сила 15");
                pages.add(new Page("07-partial-requirements.png",partial,complete));
            }finally{RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(original);}
            // Test both vanilla entry points and the old custom chat packet receiver.
            var recipe=mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("stick")).orElseThrow();
            mc.getToasts().clear();RecipeToast.addOrUpdate(mc.getToasts(),recipe);mc.getToasts().addToast(new RecipeToast(recipe));
            check(checks,mc.getToasts().getToast(RecipeToast.class,Toast.NO_TOKEN)==null,"both standard and directly queued recipe notifications are completely blocked");
            try{
                var f=mc.gui.getChat().getClass().getDeclaredField("allMessages");f.setAccessible(true);int before=((List<?>)f.get(mc.gui.getChat())).size();
                ModNetworking.clientRecipe.accept(new ModNetworking.RecipeUnlock(Component.literal("Test recipe")));
                check(checks,((List<?>)f.get(mc.gui.getChat())).size()==before,"a legacy recipe unlock packet cannot add any chat message");
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }finally{d.load(saved);}
    }
    public static boolean tick(Minecraft mc,JsonArray checks)throws Exception{
        if(done)return true;
        if(!initialized){initialize(mc,checks);initialized=true;mc.setScreen(new Preview(pages.get(0)));return false;}
        if(++age<8)return false;
        var folder=mc.gameDirectory.toPath().resolve("screenshots/requirements-2.3.10");Files.createDirectories(folder);
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder.resolve(pages.get(page).file));}
        page++;age=0;if(page<pages.size()){mc.setScreen(new Preview(pages.get(page)));return false;}
        mc.setScreen(null);done=true;return true;
    }
    private static final class Preview extends Screen {
        private final Page page;
        private Preview(Page page){super(Component.literal("Требования к использованию"));this.page=page;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
            g.fill(0,0,width,height,0xED211C18);g.drawCenteredString(font,title,width/2,20,0xEDCD87);
            draw(g,page.left,24);draw(g,page.right,width/2+8);
        }
        private void draw(GuiGraphics g,Panel panel,int x){
            g.drawString(font,panel.label,x,55,0xFFFFFF,false);g.renderItem(panel.stack,x,77);
            g.renderComponentTooltip(font,panel.tooltip,x,108);
        }
    }
    private RequirementsVisualChecks(){}
}
