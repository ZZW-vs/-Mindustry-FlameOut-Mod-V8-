package flame;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class FlameSettings{
    public static final String keyAutoRestart = "flame-autorestart";
    public static final String keyRestartTime = "flame-restarttime";
    public static final String keyUseOriginalSprites = "flame-originalsprites";

    public static boolean autoRestart = true;
    public static float restartTime = 25f;
    public static boolean useOriginalSprites = false;

    static BaseDialog dialog;
    static int currentTab = 0;
    static Table contentTable;

    public static void load(){
        autoRestart = Core.settings.getBool(keyAutoRestart, true);
        restartTime = Core.settings.getFloat(keyRestartTime, 25f);
        useOriginalSprites = Core.settings.getBool(keyUseOriginalSprites, false);
    }

    public static void save(){
        Core.settings.put(keyAutoRestart, autoRestart);
        Core.settings.put(keyRestartTime, restartTime);
        Core.settings.put(keyUseOriginalSprites, useOriginalSprites);
    }

    public static void showDialog(){
        if(dialog == null){
            dialog = new BaseDialog("FlameOut 设置");
            rebuild();
        }
        dialog.show();
    }

    static void rebuild(){
        dialog.cont.clear();
        dialog.buttons.clear();

        Table main = new Table();
        main.margin(10f);

        Table tabBar = new Table();
        tabBar.button("剧情设置", Styles.flatt, () -> {
            currentTab = 0;
            rebuildContent();
        }).size(140f, 40f).pad(4f);
        tabBar.button("键位设置", Styles.flatt, () -> {
            currentTab = 1;
            rebuildContent();
        }).size(140f, 40f).pad(4f);

        main.add(tabBar).fillX().row();

        contentTable = new Table();
        contentTable.marginTop(10f);
        rebuildContent();
        main.add(contentTable).fillX().row();

        dialog.cont.add(main).fillX().row();
        dialog.addCloseButton();
    }

    static void rebuildContent(){
        contentTable.clearChildren();
        if(currentTab == 0){
            buildStoryTab(contentTable);
        }else{
            FlameKeybinds.rebuildTable(contentTable);
        }
    }

    static void buildStoryTab(Table t){
        t.add("剧情设置").fontScale(1.2f).left().padBottom(10f).row();

        t.left();

        t.check("自动重启游戏", autoRestart, b -> {
            autoRestart = b;
            save();
        }).left().padBottom(10f).row();

        t.add("自动重启时间(秒): ").left();
        t.field(restartTime + "", s -> {
            try{
                float v = Float.parseFloat(s);
                if(v < 1f) v = 1f;
                if(v > 300f) v = 300f;
                restartTime = v;
                save();
            }catch(Exception ignored){}
        }).width(120f).left().row();

        t.row();
        t.add("贴图设置").fontScale(1.1f).left().padTop(15f).padBottom(8f).row();

        t.check("使用原版剧情贴图", useOriginalSprites, b -> {
            useOriginalSprites = b;
            save();
        }).left().padBottom(4f).row();
        t.add("重启游戏后生效").left().padBottom(10f).color(Color.lightGray).row();

        t.row();
        t.add("提示：修改后立即生效（贴图需重启）").left().padTop(20f).color(Color.lightGray).row();
    }
}
