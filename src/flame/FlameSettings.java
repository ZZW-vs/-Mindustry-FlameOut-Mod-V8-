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

public class FlameSettings{
    public static final String keyAutoRestart = "flame-autorestart";
    public static final String keyRestartTime = "flame-restarttime";
    public static final String keyUseOriginalSprites = "flame-originalsprites";
    public static final String keyBloodColor = "flame-bloodcolor";
    public static final String keyApathySpawnEmpathy = "flame-apathy-spawn-empathy";
    public static final String keyEmpathyRespawn = "flame-empathy-respawn";
    public static final String keySpriteScaleX = "flame-sprite-scalex";
    public static final String keySpriteScaleY = "flame-sprite-scaley";
    public static final String keyDisableStory = "flame-disable-story";
    public static final String keyDisableStoryKeys = "flame-disable-story-keys";
    public static final String keyMobileControls = "flame-mobile-controls";
    public static final String keyMobilePosX = "flame-mobile-posx";
    public static final String keyMobilePosY = "flame-mobile-posy";
    public static final String keyMobileMode = "flame-mobile-mode";
    public static final String keyShowHudInfo = "flame-show-hud-info";

    public static boolean autoRestart = true;
    public static float restartTime = 25f;
    public static boolean useOriginalSprites = false;
    public static int bloodColorIndex = 1;
    public static boolean apathySpawnEmpathy = true;
    public static boolean empathyRespawn = true;
    public static float spriteScaleX = 1f;
    public static float spriteScaleY = 1f;
    public static boolean disableStory = false;
    public static boolean disableStoryKeys = false;
    public static boolean mobileControls = true;
    public static boolean mobileMode = false;
    public static boolean showHudInfo = false;

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

    static Table settingsTable;

    public static void load(){
        autoRestart = Core.settings.getBool(keyAutoRestart, true);
        restartTime = Core.settings.getFloat(keyRestartTime, 25f);
        useOriginalSprites = Core.settings.getBool(keyUseOriginalSprites, false);
        bloodColorIndex = Core.settings.getInt(keyBloodColor, 1);
        apathySpawnEmpathy = Core.settings.getBool(keyApathySpawnEmpathy, true);
        empathyRespawn = Core.settings.getBool(keyEmpathyRespawn, true);
        spriteScaleX = Core.settings.getFloat(keySpriteScaleX, 1f);
        spriteScaleY = Core.settings.getFloat(keySpriteScaleY, 1f);
        disableStory = Core.settings.getBool(keyDisableStory, false);
        disableStoryKeys = Core.settings.getBool(keyDisableStoryKeys, false);
        mobileControls = Core.settings.getBool(keyMobileControls, true);
        mobileMode = Core.settings.getBool(keyMobileMode, false);
        showHudInfo = Core.settings.getBool(keyShowHudInfo, false);
        applyBloodColor();
    }

    public static void save(){
        Core.settings.put(keyAutoRestart, autoRestart);
        Core.settings.put(keyRestartTime, restartTime);
        Core.settings.put(keyUseOriginalSprites, useOriginalSprites);
        Core.settings.put(keyBloodColor, bloodColorIndex);
        Core.settings.put(keyApathySpawnEmpathy, apathySpawnEmpathy);
        Core.settings.put(keyEmpathyRespawn, empathyRespawn);
        Core.settings.put(keySpriteScaleX, spriteScaleX);
        Core.settings.put(keySpriteScaleY, spriteScaleY);
        Core.settings.put(keyDisableStory, disableStory);
        Core.settings.put(keyDisableStoryKeys, disableStoryKeys);
        Core.settings.put(keyMobileControls, mobileControls);
        Core.settings.put(keyMobileMode, mobileMode);
        Core.settings.put(keyShowHudInfo, showHudInfo);
    }

    public static void applyBloodColor(){
        if(bloodColorIndex >= 0 && bloodColorIndex < bloodColors.length){
            FlamePal.blood.set(bloodColors[bloodColorIndex]);
        }
    }

    public static void showSettings(){
        Vars.ui.settings.show();
    }

    public static void rebuildAll(){
        if(settingsTable != null){
            buildAll(settingsTable);
        }
    }

    public static void buildAll(Table t){
        settingsTable = t;
        t.clearChildren();
        t.left();
        t.defaults().left();

        // === 兼容性 ===
        t.add("兼容性").fontScale(1.15f).color(Color.acid).padTop(4f).row();
        t.image().fillX().height(3f).color(Color.gray).padBottom(8f).row();

        t.check("手机版兼容模式", mobileMode, b -> {
            mobileMode = b;
            if(b){
                mobileControls = true;
                disableStoryKeys = true;
            }
            save();
            if(flame.special.MobileControls.buttonTable != null){
                flame.special.MobileControls.enabled = mobileControls;
                if(mobileControls){
                    flame.special.MobileControls.build();
                }else{
                    flame.special.MobileControls.dispose();
                }
            }
            rebuildAll();
        }).padBottom(4f).row();
        t.add("启用后自动开启虚拟按键、禁用剧情快捷键、隐藏键位设置").color(Color.lightGray).padBottom(6f).row();

        t.check("显示虚拟按键", mobileControls, b -> {
            mobileControls = b;
            save();
            if(flame.special.MobileControls.buttonTable != null){
                flame.special.MobileControls.enabled = b;
                if(b){
                    flame.special.MobileControls.build();
                }else{
                    flame.special.MobileControls.dispose();
                }
            }
        }).padBottom(4f).row();
        t.add("手机版显示可拖动的虚拟按键面板").color(Color.lightGray).padBottom(6f).row();

        t.check("显示终端信息", showHudInfo, b -> {
            showHudInfo = b;
            save();
        }).padBottom(4f).row();
        t.add("游戏内左上角显示简洁状态信息").color(Color.lightGray).padBottom(10f).row();

        // === 单位 ===
        t.add("单位").fontScale(1.15f).color(Color.acid).padTop(10f).row();
        t.image().fillX().height(3f).color(Color.gray).padBottom(8f).row();

        t.check("冷漠死后生成共鸣", apathySpawnEmpathy, b -> {
            apathySpawnEmpathy = b;
            save();
        }).padBottom(6f).row();

        t.check("共鸣跨地图重生", empathyRespawn, b -> {
            empathyRespawn = b;
            save();
        }).padBottom(6f).row();

        t.button("重置共鸣生成器", Styles.flatt, () -> {
            flame.unit.empathy.EmpathyDamage.resetSpawner();
        }).size(200f, 36f).padBottom(4f).row();
        t.add("清除当前共鸣生成状态").color(Color.lightGray).padBottom(10f).row();

        // === 剧情 ===
        t.add("剧情").fontScale(1.15f).color(Color.acid).padTop(10f).row();
        t.image().fillX().height(3f).color(Color.gray).padBottom(8f).row();

        t.check("禁用剧情", disableStory, b -> {
            disableStory = b;
            save();
        }).padBottom(6f).row();

        t.check("禁用剧情快捷键", disableStoryKeys, b -> {
            disableStoryKeys = b;
            save();
        }).padBottom(6f).row();

        t.check("自动重启游戏", autoRestart, b -> {
            autoRestart = b;
            save();
        }).padBottom(6f).row();

        Table restartRow = new Table();
        restartRow.left();
        restartRow.add("自动重启时间(秒): ").left();
        restartRow.field(restartTime + "", s -> {
            try{
                float v = Float.parseFloat(s);
                if(v < 1f) v = 1f;
                if(v > 300f) v = 300f;
                restartTime = v;
                save();
            }catch(Exception ignored){}
        }).width(100f);
        t.add(restartRow).padBottom(6f).row();

        t.check("使用原版剧情贴图", useOriginalSprites, b -> {
            useOriginalSprites = b;
            save();
        }).padBottom(4f).row();
        t.add("重启游戏后生效").color(Color.lightGray).padBottom(10f).row();

        // === 视觉 ===
        t.add("视觉").fontScale(1.15f).color(Color.acid).padTop(10f).row();
        t.image().fillX().height(3f).color(Color.gray).padBottom(8f).row();

        t.add("血液颜色:").padBottom(6f).row();

        Table colors = new Table();
        colors.left();
        for(int i = 0; i < bloodColorNames.length; i++){
            int idx = i;
            Button b = colors.button(bloodColorNames[i], Styles.flatt, () -> {
                bloodColorIndex = idx;
                save();
                applyBloodColor();
                rebuildAll();
            }).size(140f, 36f).pad(2f).left().get();
            b.update(() -> {
                if(bloodColorIndex == idx){
                    b.setColor(1f, 1f, 1f, 1f);
                }else{
                    b.setColor(0.7f, 0.7f, 0.7f, 1f);
                }
            });
            if((i + 1) % 2 == 0) colors.row();
        }
        t.add(colors).padBottom(10f).row();

        // === 键位 ===
        t.add("键位").fontScale(1.15f).color(Color.acid).padTop(10f).row();
        t.image().fillX().height(3f).color(Color.gray).padBottom(8f).row();

        if(mobileMode){
            t.add("手机版兼容模式下键位设置已禁用").color(Color.lightGray).padBottom(6f).row();
            t.add("请使用游戏内虚拟按键").color(Color.lightGray).padBottom(10f).row();
        }else{
            FlameKeybinds.rebuildTable(t);
        }
    }
}
