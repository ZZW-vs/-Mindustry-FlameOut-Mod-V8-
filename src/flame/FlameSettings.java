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
        ui.settings.show();
    }

    public static void rebuildAll(){
        if(rootTable != null) buildAll(rootTable);
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
            if(b){ mobileControls = true; disableStoryKeys = true; }
            save();
            if(MobileControls.buttonTable != null){
                MobileControls.enabled = mobileControls;
                if(mobileControls) MobileControls.build(); else MobileControls.dispose();
            }
            rebuildAll();
        }).left().row();
        t.add("  启用后自动开启虚拟按键、禁用剧情快捷键、隐藏键位设置").left().color(Color.lightGray).row();

        t.check("显示虚拟按键", mobileControls, b -> {
            mobileControls = b;
            save();
            if(MobileControls.buttonTable != null){
                MobileControls.enabled = b;
                if(b) MobileControls.build(); else MobileControls.dispose();
            }
        }).left().row();
        t.add("  手机版显示可拖动的虚拟按键面板").left().color(Color.lightGray).padBottom(4f).row();

        t.check("显示终端信息", showHudInfo, b -> {
            showHudInfo = b;
            save();
        }).left().row();
        t.add("  游戏内左上角显示简洁状态信息").left().color(Color.lightGray).padBottom(4f).row();

        // === 单位 ===
        t.add("[accent]单位[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        t.check("冷漠死后生成共鸣", apathySpawnEmpathy, b -> { apathySpawnEmpathy = b; save(); }).left().row();
        t.check("共鸣跨地图重生", empathyRespawn, b -> { empathyRespawn = b; save(); }).left().row();
        t.button("重置共鸣生成器", Styles.flatt, () -> {
            flame.unit.empathy.EmpathyDamage.resetSpawner();
        }).size(200f, 36f).left().row();
        t.add("  清除当前共鸣生成状态").left().color(Color.lightGray).padBottom(4f).row();

        // === 剧情 ===
        t.add("[accent]剧情[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

        t.check("禁用剧情", disableStory, b -> { disableStory = b; save(); }).left().row();
        t.check("禁用剧情快捷键", disableStoryKeys, b -> { disableStoryKeys = b; save(); }).left().row();
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
            t.add("  手机版兼容模式下键位设置已禁用").left().color(Color.lightGray).row();
            t.add("  请使用游戏内虚拟按键面板").left().color(Color.lightGray).padBottom(4f).row();
        }else{
            // 用子表格承载键位设置，避免 rebuildTable 清空整个表格
            Table keybindTable = new Table();
            keybindTable.left();
            t.add(keybindTable).left().fillX().row();
            FlameKeybinds.rebuildTable(keybindTable);
        }

        // === 手机版快捷操作（仅手机版兼容模式） ===
        if(mobileMode){
            t.add("[accent]快捷操作[]").left().padTop(10f).row();
            t.image().fillX().height(2f).color(Color.gray).padBottom(6f).row();

            Table btns1 = new Table();
            btns1.left();
            btns1.button("Z - 重置", Styles.flatt, () -> simulateAction("reset")).size(120f, 36f).pad(2f);
            btns1.button("X - 前进", Styles.flatt, () -> simulateAction("next")).size(120f, 36f).pad(2f);
            t.add(btns1).left().row();

            Table btns2 = new Table();
            btns2.left();
            btns2.button("V - 启动", Styles.flatt, () -> simulateAction("start")).size(120f, 36f).pad(2f);
            btns2.button("C - 退出", Styles.flatt, () -> simulateAction("quit")).size(120f, 36f).pad(2f);
            t.add(btns2).left().row();

            Table btns3 = new Table();
            btns3.left();
            btns3.button("H - 贴图", Styles.flatt, () -> simulateAction("sprites")).size(120f, 36f).pad(2f);
            Button ffBtn = new Button(Styles.flatToggleMenut){
                { update(() -> { if(isPressed() && !disableStory && !disableStoryKeys) simulateAction("fastforward"); }); }
            };
            ffBtn.add("B - 快进").get().setFontScale(0.8f);
            btns3.add(ffBtn).size(120f, 36f).pad(2f);
            t.add(btns3).left().row();
        }

        // === 终端信息 ===
        t.add("[accent]终端信息[]").left().padTop(10f).row();
        t.image().fillX().height(2f).color(Color.gray).padBottom(4f).row();

        Label hudLabel = new Label("");
        hudLabel.setFontScale(0.7f);
        hudLabel.setColor(Color.lightGray);
        hudLabel.setAlignment(Align.left);
        Label finalHudLabel = hudLabel;
        hudLabel.update(() -> {
            if(showHudInfo) finalHudLabel.setText(getHudInfoText());
            else finalHudLabel.setText("[gray](终端信息已关闭，请在上方开启)[]");
        });
        t.add(hudLabel).left().width(400f).row();
    }

    static void simulateAction(String action){
        if(disableStory) return;
        switch(action){
            case "reset" -> { Log.info("[FlameOut] 重置剧情"); SpecialMain.resetStory(); }
            case "next" -> { if(SpecialMain.getStage() < 5){ SpecialMain.increment(false); Log.info("[FlameOut] 前进到阶段 " + SpecialMain.getStage()); } }
            case "start" -> { Log.info("[FlameOut] 启动剧情"); SpecialMain.startStory(); }
            case "quit" -> { Log.info("[FlameOut] 退出剧情"); SpecialMain.resetStory(); Core.app.exit(); }
            case "sprites" -> { if(SecretSpritesMenu.dialog == null) SecretSpritesMenu.load(); SecretSpritesMenu.rebuild(); SecretSpritesMenu.dialog.show(); }
            case "fastforward" -> SpecialMain.fastForward();
        }
    }

    static String getHudInfoText(){
        int stage = SpecialMain.getStage();
        String stageName = switch(stage){
            case 0 -> "未开始"; case 1 -> "阶段1"; case 2 -> "阶段2";
            case 3 -> "阶段3"; case 4 -> "阶段4"; case 5 -> "阶段5";
            default -> "已完成";
        };
        StringBuilder sb = new StringBuilder();
        sb.append("[accent]FlameOut[]\n");
        sb.append("剧情: ").append(stageName);
        if(disableStory) sb.append(" [gray](已禁用)[]");
        else if(SpecialMain.isActive()) sb.append(" [gray](运行中)[]");
        sb.append("\n虚拟按键: ").append(mobileControls ? "[green]开[]" : "[gray]关[]");
        if(mobile) sb.append("\n[gray](手机版)[]");
        return sb.toString();
    }
}
