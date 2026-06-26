package flame.special;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class SecretSpritesMenu{
    public static Seq<PlacedSprite> placed = new Seq<>();
    static BaseDialog dialog;
    static float selectedSize = 8f;
    static float selectedRot = 0f;
    static float selectedScaleX = 1f;
    static float selectedScaleY = 1f;

    static String[] spriteNames = {
        "main", "ball", "hug", "cat",
        "bunny0", "bunny1", "bunny2", "bunny3", "bunny4",
        "flower", "tree"
    };

    public static void load(){
        selectedSize = 8f;
        selectedRot = 0f;
        selectedScaleX = FlameSettings.spriteScaleX;
        selectedScaleY = FlameSettings.spriteScaleY;
        dialog = new BaseDialog("隐藏贴图");
        rebuild();
    }

    static void rebuild(){
        dialog.cont.clear();
        dialog.buttons.clear();

        Table grid = new Table();
        int cols = 4;
        int idx = 0;

        for(String name : spriteNames){
            TextureRegion reg = SpecialMain.regions.get(name);
            if(reg == null) continue;

            ImageButton button = grid.button(Tex.whiteui, Styles.clearTogglei, 64f, () -> {
                placeSprite(name);
            }).size(80f).pad(4f).get();

            button.getStyle().imageUp = new TextureRegionDrawable(reg);
            button.getImage().setScaling(Scaling.fit);
            button.update(() -> button.setChecked(false));

            if(++idx % cols == 0) grid.row();
        }

        ScrollPane pane = new ScrollPane(grid, Styles.smallPane);
        pane.setScrollingDisabled(true, false);
        dialog.cont.add(pane).width(400f).maxHeight(400f).row();

        Table settings = new Table(Tex.button);
        settings.margin(10f);

        settings.add("大小: ").left();
        settings.field(selectedSize + "", s -> {
            try{
                selectedSize = Float.parseFloat(s);
                if(selectedSize < 1f) selectedSize = 1f;
                if(selectedSize > 100f) selectedSize = 100f;
            }catch(Exception ignored){}
        }).width(80f).left();
        settings.row();

        settings.add("旋转: ").left();
        settings.field(selectedRot + "", s -> {
            try{
                selectedRot = Float.parseFloat(s);
            }catch(Exception ignored){}
        }).width(80f).left();
        settings.row();

        settings.add("横向拉伸: ").left();
        settings.field(selectedScaleX + "", s -> {
            try{
                selectedScaleX = Float.parseFloat(s);
                if(selectedScaleX < 0.1f) selectedScaleX = 0.1f;
                if(selectedScaleX > 10f) selectedScaleX = 10f;
                FlameSettings.spriteScaleX = selectedScaleX;
                FlameSettings.save();
            }catch(Exception ignored){}
        }).width(80f).left();
        settings.row();

        settings.add("纵向拉伸: ").left();
        settings.field(selectedScaleY + "", s -> {
            try{
                selectedScaleY = Float.parseFloat(s);
                if(selectedScaleY < 0.1f) selectedScaleY = 0.1f;
                if(selectedScaleY > 10f) selectedScaleY = 10f;
                FlameSettings.spriteScaleY = selectedScaleY;
                FlameSettings.save();
            }catch(Exception ignored){}
        }).width(80f).left();
        settings.row();

        settings.button("清除所有", () -> placed.clear()).colspan(2).fillX().padTop(8f);

        dialog.cont.add(settings).width(400f).padTop(10f).row();

        dialog.addCloseButton();
    }

    static void placeSprite(String name){
        TextureRegion reg = SpecialMain.regions.get(name);
        if(reg == null || Vars.player.unit() == null) return;

        Unit u = Vars.player.unit();
        placed.add(new PlacedSprite(reg, name, u.x, u.y, selectedSize, selectedRot, selectedScaleX, selectedScaleY));
    }

    public static void update(){
        if(FlameKeybinds.tap("key-sprites") && Vars.state.isGame()){
            if(dialog == null) load();
            rebuild();
            dialog.show();
        }
    }

    public static void draw(){
        if(!Vars.state.isGame()) return;

        for(PlacedSprite p : placed){
            Draw.z(Layer.blockUnder - 0.5f);
            Draw.color();
            float width = p.size * Vars.tilesize * p.scaleX;
            float height = p.size * Vars.tilesize * p.scaleY;
            Draw.rect(p.region, p.x, p.y, width, height, p.rot);
        }
    }

    public static class PlacedSprite{
        public TextureRegion region;
        public String name;
        public float x, y;
        public float size;
        public float rot;
        public float scaleX;
        public float scaleY;

        public PlacedSprite(TextureRegion region, String name, float x, float y, float size, float rot, float scaleX, float scaleY){
            this.region = region;
            this.name = name;
            this.x = x;
            this.y = y;
            this.size = size;
            this.rot = rot;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }
    }
}
