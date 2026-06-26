package flame;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.core.GameState.*;

public class FlameSettings{
    public static final String keyAutoRestart = "flame-autorestart";
    public static final String keyRestartTime = "flame-restarttime";
    public static final String keyUseOriginalSprites = "flame-originalsprites";
    public static final String keyBloodColor = "flame-bloodcolor";
    public static final String keyApathySpawnEmpathy = "flame-apathy-spawn-empathy";
    public static final String keyEmpathyRespawn = "flame-empathy-respawn";

    public static boolean autoRestart = true;
    public static float restartTime = 25f;
    public static boolean useOriginalSprites = false;
    public static int bloodColorIndex = 1;
    public static boolean apathySpawnEmpathy = true;
    public static boolean empathyRespawn = true;

    static final String[] bloodColorNames = {
        "血色(原版)",
        "白色",
        "黄色",
        "黑色",
        "蓝色",
        "紫色",
        "橙色",
        "绿色"
    };

    static final Color[] bloodColors = {
        new Color(0.6f, 0.05f, 0.05f),
        new Color(0.95f, 0.95f, 0.95f),
        new Color(1f, 1f, 0.2f),
        new Color(0.1f, 0.1f, 0.1f),
        new Color(0.2f, 0.4f, 1f),
        new Color(0.6f, 0.2f, 0.8f),
        new Color(1f, 0.5f, 0.1f),
        new Color(0.2f, 0.8f, 0.3f)
    };

    static BaseDialog dialog;
    static int currentTab = 0;
    static Table contentTable;

    public static void load(){
        autoRestart = Core.settings.getBool(keyAutoRestart, true);
        restartTime = Core.settings.getFloat(keyRestartTime, 25f);
        useOriginalSprites = Core.settings.getBool(keyUseOriginalSprites, false);
        bloodColorIndex = Core.settings.getInt(keyBloodColor, 1);
        apathySpawnEmpathy = Core.settings.getBool(keyApathySpawnEmpathy, true);
        empathyRespawn = Core.settings.getBool(keyEmpathyRespawn, true);
        applyBloodColor();
    }

    public static void save(){
        Core.settings.put(keyAutoRestart, autoRestart);
        Core.settings.put(keyRestartTime, restartTime);
        Core.settings.put(keyUseOriginalSprites, useOriginalSprites);
        Core.settings.put(keyBloodColor, bloodColorIndex);
        Core.settings.put(keyApathySpawnEmpathy, apathySpawnEmpathy);
        Core.settings.put(keyEmpathyRespawn, empathyRespawn);
    }

    public static void applyBloodColor(){
        if(bloodColorIndex >= 0 && bloodColorIndex < bloodColors.length){
            FlamePal.blood.set(bloodColors[bloodColorIndex]);
        }
    }

    static boolean wasPaused = false;

    public static void showDialog(){
        if(dialog == null){
            dialog = new BaseDialog("FlameOut 设置");
            rebuild();
            dialog.shown(() -> {
                if(Vars.state.isGame()){
                    wasPaused = Vars.state.isPaused();
                    if(!wasPaused){
                        Vars.state.set(State.paused);
                    }
                }
            });
            dialog.hidden(() -> {
                if(Vars.state.isGame() && !wasPaused){
                    Vars.state.set(State.playing);
                }
            });
        }
        dialog.show();
    }

    static void rebuild(){
        dialog.cont.clear();
        dialog.buttons.clear();

        Table main = new Table();
        main.margin(10f);

        Table tabBar = new Table();
        tabBar.button("单位设置", Styles.flatt, () -> {
            currentTab = 0;
            rebuildContent();
        }).size(140f, 40f).pad(4f);
        tabBar.button("剧情设置", Styles.flatt, () -> {
            currentTab = 1;
            rebuildContent();
        }).size(140f, 40f).pad(4f);
        tabBar.button("视觉设置", Styles.flatt, () -> {
            currentTab = 2;
            rebuildContent();
        }).size(140f, 40f).pad(4f);
        tabBar.button("键位设置", Styles.flatt, () -> {
            currentTab = 3;
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
            buildUnitTab(contentTable);
        }else if(currentTab == 1){
            buildStoryTab(contentTable);
        }else if(currentTab == 2){
            buildVisualTab(contentTable);
        }else{
            FlameKeybinds.rebuildTable(contentTable);
        }
    }

    static void buildUnitTab(Table t){
        t.add("单位设置").fontScale(1.2f).left().padBottom(10f).row();

        t.left();

        t.check("冷漠死后生成共鸣", apathySpawnEmpathy, b -> {
            apathySpawnEmpathy = b;
            save();
        }).left().padBottom(10f).row();

        t.check("共鸣跨地图重生", empathyRespawn, b -> {
            empathyRespawn = b;
            save();
        }).left().padBottom(10f).row();

        t.row();
        t.add("提示：修改后立即生效").left().padTop(20f).color(Color.lightGray).row();
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
    }

    static void buildVisualTab(Table t){
        t.add("视觉设置").fontScale(1.2f).left().padBottom(10f).row();

        t.left();

        t.add("血液颜色: ").left().padBottom(8f).row();

        Table colors = new Table();
        colors.left();
        for(int i = 0; i < bloodColorNames.length; i++){
            int idx = i;
            Button b = colors.button(bloodColorNames[i], Styles.flatt, () -> {
                bloodColorIndex = idx;
                save();
                applyBloodColor();
                rebuildContent();
            }).size(180f, 40f).pad(2f).left().get();
            b.update(() -> {
                if(bloodColorIndex == idx){
                    b.setColor(1f, 1f, 1f, 1f);
                }else{
                    b.setColor(0.7f, 0.7f, 0.7f, 1f);
                }
            });
            if((i + 1) % 2 == 0) colors.row();
        }
        ScrollPane pane = new ScrollPane(colors, Styles.smallPane);
        pane.setScrollingDisabled(true, false);
        t.add(pane).width(420f).maxHeight(300f).left().row();

        t.row();
        t.add("提示：修改后立即生效").left().padTop(20f).color(Color.lightGray).row();
    }
}
