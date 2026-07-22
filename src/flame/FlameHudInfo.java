package flame;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import flame.special.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class FlameHudInfo{
    static Table infoTable;
    static Label infoLabel;
    static float updateTimer = 0f;

    public static void build(){
        if(infoTable != null){
            infoTable.remove();
            infoTable = null;
        }

        infoTable = new Table();
        infoTable.top().left();
        infoTable.margin(8f);

        infoLabel = new Label("");
        infoLabel.setColor(Color.white);
        infoLabel.setFontScale(0.7f);

        Table bg = new Table(Tex.buttonTrans);
        bg.margin(6f);
        bg.add(infoLabel);
        infoTable.add(bg);

        infoTable.update(() -> {
            infoTable.visible = Vars.state.isGame();
        });

        ui.hudGroup.addChild(infoTable);
    }

    public static void update(){
        updateTimer += Time.delta;

        if(infoTable == null && state.isGame()){
            build();
        }

        if(infoTable != null && !state.isGame()){
            // 菜单状态下清理
            infoTable.remove();
            infoTable = null;
            infoLabel = null;
        }

        // 每20帧更新一次文本
        if(infoLabel != null && updateTimer >= 20f){
            updateTimer = 0f;
            infoLabel.setText(getInfoText());
        }
    }

    static String getInfoText(){
        StringBuilder sb = new StringBuilder();

        // === 标题 ===
        sb.append("[accent]FlameOut 终端[]\n");

        // === 剧情状态 ===
        int stage = SpecialMain.getStage();
        String stageName;
        switch(stage){
            case 0 -> stageName = "未开始";
            case 1 -> stageName = "阶段1";
            case 2 -> stageName = "阶段2";
            case 3 -> stageName = "阶段3";
            case 4 -> stageName = "阶段4";
            case 5 -> stageName = "阶段5";
            default -> stageName = "已完成";
        }

        sb.append("[gray]剧情:[] ").append(stageName);
        if(FlameSettings.disableStory){
            sb.append(" [scarlet](已禁用)[]");
        }else if(SpecialMain.isActive()){
            sb.append(" [green](运行中)[]");
        }else if(stage > 0){
            sb.append(" [yellow](待启动)[]");
        }
        sb.append("\n");

        // 剧情时间
        float curTime = SpecialMain.getStoryTime();
        if(curTime >= 0f){
            sb.append("[gray]剧情时间:[] ").append(formatTime(curTime)).append("\n");
        }

        // === 共鸣生成器 ===
        try{
            boolean spawnerActive = EmpathyDamage.isSpawnerActive();
            float countdown = EmpathyDamage.getSpawnerCountdown();
            int empathyCount = EmpathyDamage.getEmpathyCount();
            sb.append("[gray]共鸣:[] ").append(empathyCount).append("只");
            if(spawnerActive){
                sb.append(" [green]生成器运行[]");
                if(countdown >= 0){
                    sb.append(" [gray]倒计时:[]").append((int)countdown).append("s");
                }
            }
            sb.append("\n");
        }catch(Exception ignored){}

        // === 游戏状态 ===
        sb.append("[gray]状态:[] ");
        if(state.isMenu()){
            sb.append("[gray]菜单[]");
        }else if(state.isGame()){
            sb.append("[green]游戏中[]");
            if(state.rules.pvp){
                sb.append(" [accent]PVP[]");
            }
            if(state.rules.infiniteResources){
                sb.append(" [accent]无限资源[]");
            }
            if(state.rules.attackMode){
                sb.append(" [accent]攻击[]");
            }
        }else if(state.isPaused()){
            sb.append("[yellow]暂停[]");
        }
        sb.append("\n");

        // 波次/敌人（仅在游戏中）
        if(state.isGame()){
            try{
                if(state.rules.waves){
                    sb.append("[gray]波次:[] ").append(state.wave);
                    int enemies = state.enemies;
                    sb.append(" [gray]敌人:[] ").append(enemies).append("\n");
                }
            }catch(Exception ignored){}
        }

        // === 玩家信息 ===
        try{
            Unit pu = player.unit();
            if(pu != null && pu.isValid()){
                sb.append("[gray]玩家:[] ");
                sb.append(pu.type == null ? "?" : pu.type.name);
                sb.append(" [gray]HP:[]").append((int)pu.health).append("/").append((int)pu.maxHealth);
                if(pu.team != null){
                    sb.append(" [gray]队:[]").append(pu.team.name == null ? pu.team.id : pu.team.name);
                }
                sb.append("\n");
            }
        }catch(Exception ignored){}

        // === 实体统计 ===
        int unitCount = Groups.unit.size();
        int bulletCount = Groups.bullet.size();
        int buildCount = 0;
        try{
            buildCount = Groups.build.size();
        }catch(Exception ignored){}
        sb.append("[gray]实体:[] 单位").append(unitCount);
        sb.append(" 建筑").append(buildCount);
        sb.append(" 弹").append(bulletCount).append("\n");

        // === 性能 ===
        sb.append("[gray]FPS:[] ");
        int fps = Core.graphics.getFramesPerSecond();
        if(fps >= 55){
            sb.append("[green]").append(fps).append("[]");
        }else if(fps >= 30){
            sb.append("[yellow]").append(fps).append("[]");
        }else{
            sb.append("[scarlet]").append(fps).append("[]");
        }

        // 内存
        try{
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long max = rt.maxMemory() / 1024 / 1024;
            sb.append(" [gray]内存:[]").append(used).append("/").append(max).append("MB");
        }catch(Exception ignored){}
        sb.append("\n");

        // === 平台/版本 ===
        sb.append("[gray]平台:[] ").append(mobile ? "[accent]手机[]" : "[accent]桌面[]");
        if(FlameSettings.mobileMode){
            sb.append(" [accent](兼容模式)[]");
        }
        sb.append("\n");

        sb.append("[gray]版本:[] ");
        try{
            String modVer = mods.getMod("flameout") != null ? mods.getMod("flameout").meta.version : "?";
            sb.append("v").append(modVer);
        }catch(Exception e){
            sb.append("?");
        }
        sb.append(" [gray]引擎:[] v").append(Version.build);
        sb.append(" [gray]Java:[] ").append(System.getProperty("java.version", "?"));

        return sb.toString();
    }

    static String formatTime(float ticks){
        int sec = (int)(ticks / 60f);
        int m = sec / 60;
        int s = sec % 60;
        if(m > 0){
            return m + "m" + s + "s";
        }
        return s + "s";
    }

    public static void dispose(){
        if(infoTable != null){
            infoTable.remove();
            infoTable = null;
        }
    }
}
