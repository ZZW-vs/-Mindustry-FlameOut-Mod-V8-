package flame;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import flame.special.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

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
    public static final String keyAutoCleanOnDeath = "flame-auto-clean-on-death";

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
    public static boolean mobileControls = false;
    public static boolean mobileMode = false;
    public static boolean autoCleanOnDeath = false;

    static final String[] bloodColorNames = {
        "血色(原版)", "白色", "黄色", "黑色",
        "蓝色", "紫色", "橙色", "绿色"
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

    static Table rootTable;

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
        mobileControls = Core.settings.getBool(keyMobileControls, false);
        mobileMode = Core.settings.getBool(keyMobileMode, false);
        autoCleanOnDeath = Core.settings.getBool(keyAutoCleanOnDeath, false);
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
        Core.settings.put(keyAutoCleanOnDeath, autoCleanOnDeath);
    }

    public static void applyBloodColor(){
        if(bloodColorIndex >= 0 && bloodColorIndex < bloodColors.length){
            FlamePal.blood.set(bloodColors[bloodColorIndex]);
        }
    }

    public static void showSettings(){
        showFlameOutDialog();
    }

    static BaseDialog flameOutDialog;
    public static void showFlameOutDialog(){
        if(flameOutDialog == null){
            flameOutDialog = new BaseDialog("FlameOut 设置");
            flameOutDialog.addCloseButton();
            flameOutDialog.cont.margin(14f);
        }
        flameOutDialog.cont.clearChildren();
        buildAll(flameOutDialog.cont);
        flameOutDialog.show();
    }

    public static void rebuildAll(){
        if(rootTable != null) buildAll(rootTable);
        if(flameOutDialog != null && flameOutDialog.isShown()){
            flameOutDialog.cont.clearChildren();
            buildAll(flameOutDialog.cont);
        }
    }

    public static void buildAll(Table t){
        rootTable = t;
        t.clearChildren();
        t.left().top();

        // === 兼容性 ===
        t.add("[accent]兼容性[]").left().padTop(6f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        t.check("手机版兼容模式", mobileMode, b -> {
            mobileMode = b;
            if(b){
                mobileControls = true;
                disableStoryKeys = true;
            }else{
                mobileControls = false;
                MobileControls.dispose();
            }
            save();
            MobileControls.enabled = mobileControls;
            if(mobileControls){
                MobileControls.build();
            }else{
                MobileControls.dispose();
            }
            rebuildAll();
        }).left().row();
        t.add("  启用后自动开启虚拟按键、禁用剧情快捷键").left().color(Color.lightGray).padBottom(4f).row();

        if(mobileMode){
            t.check("显示虚拟按键", mobileControls, b -> {
                mobileControls = b;
                save();
                MobileControls.enabled = b;
                if(b){
                    MobileControls.build();
                }else{
                    MobileControls.dispose();
                }
            }).left().row();
            t.add("  可拖动到任意位置，位置自动保存").left().color(Color.lightGray).padBottom(4f).row();
        }

        // === 单位 ===
        t.add("[accent]单位[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        t.check("冷漠死后生成共鸣", apathySpawnEmpathy, b -> { apathySpawnEmpathy = b; save(); }).left().row();
        t.check("共鸣跨地图重生", empathyRespawn, b -> { empathyRespawn = b; save(); }).left().row();
        t.button("重置共鸣生成器", Styles.flatt, () -> {
            flame.unit.empathy.EmpathyDamage.resetSpawner();
        }).size(200f, 36f).left().row();
        t.add("  清除当前共鸣生成状态").left().color(Color.lightGray).padBottom(4f).row();

        t.check("单位清除器被摧毁时自动清除", autoCleanOnDeath, b -> { autoCleanOnDeath = b; save(); }).left().row();
        t.add("  开启后单位清除器方块被打死会自动清除所有单位").left().color(Color.lightGray).padBottom(4f).row();

        // === 剧情 ===
        t.add("[accent]剧情[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        t.check("禁用剧情", disableStory, b -> { disableStory = b; save(); }).left().row();

        if(!mobileMode){
            t.check("禁用剧情快捷键", disableStoryKeys, b -> { disableStoryKeys = b; save(); }).left().row();
        }

        t.check("自动重启游戏", autoRestart, b -> { autoRestart = b; save(); }).left().row();

        Table restartRow = new Table();
        restartRow.left();
        restartRow.add("自动重启时间(秒): ").left();
        restartRow.field(restartTime + "", s -> {
            try{ float v = Float.parseFloat(s); if(v<1f)v=1f; if(v>300f)v=300f; restartTime=v; save(); }catch(Exception ignored){}
        }).width(80f);
        t.add(restartRow).left().row();

        t.check("使用原版剧情贴图", useOriginalSprites, b -> { useOriginalSprites = b; save(); }).left().row();
        t.add("  重启游戏后生效").left().color(Color.lightGray).padBottom(4f).row();

        // === 视觉 ===
        t.add("[accent]视觉[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        t.add("血液颜色:").left().row();
        Table colors = new Table();
        colors.left();
        for(int i = 0; i < bloodColorNames.length; i++){
            int idx = i;
            Button b = colors.button(bloodColorNames[i], Styles.flatt, () -> {
                bloodColorIndex = idx; save(); applyBloodColor(); rebuildAll();
            }).size(100f, 32f).pad(2f).get();
            b.update(() -> b.setColor(bloodColorIndex == idx ? Color.white : Color.gray));
            if((i+1) % 4 == 0) colors.row();
        }
        t.add(colors).left().padBottom(4f).row();

        // === 键位 ===
        t.add("[accent]键位[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        if(mobileMode){
            t.add("  手机兼容模式下键位设置已禁用").left().color(Color.lightGray).row();
            t.add("  请使用游戏内虚拟按键面板").left().color(Color.lightGray).padBottom(4f).row();
        }else{
            Table keybindTable = new Table();
            keybindTable.left();
            t.add(keybindTable).left().fillX().row();
            FlameKeybinds.rebuildTable(keybindTable);
        }
    }
}
