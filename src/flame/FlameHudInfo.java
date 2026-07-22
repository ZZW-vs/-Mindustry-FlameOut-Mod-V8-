package flame;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import flame.special.*;
import mindustry.*;
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
        infoLabel.setFontScale(0.8f);

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

        // 每30帧更新一次文本
        if(infoLabel != null && updateTimer >= 30f){
            updateTimer = 0f;
            infoLabel.setText(getInfoText());
        }
    }

    static String getInfoText(){
        StringBuilder sb = new StringBuilder();

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

        sb.append("[accent]FlameOut[]\n");
        sb.append("剧情: ").append(stageName);

        if(FlameSettings.disableStory){
            sb.append(" [gray](已禁用)[]");
        }else if(SpecialMain.isActive()){
            sb.append(" [gray](运行中)[]");
        }

        return sb.toString();
    }

    public static void dispose(){
        if(infoTable != null){
            infoTable.remove();
            infoTable = null;
        }
    }
}
