package flame.special;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 单位清除器方块：1x1，点击弹出按钮清除场上所有非玩家单位（含共鸣单位）。
 * 若 FlameSettings.autoCleanOnDeath 开启，方块被摧毁时自动触发清除。
 */
public class UnitCleaner extends Block{

    public UnitCleaner(String name){
        super(name);
        size = 1;
        health = 200;
        solid = false;
        destructible = true;
        breakable = true;
        configurable = true;
        update = true;
        category = Category.effect;
        buildVisibility = BuildVisibility.sandboxOnly;
        canOverdrive = false;
        buildType = CleanerBuild::new;
    }

    @Override
    public void load(){
        super.load();
        // 使用游戏内置贴图作为占位
        region = Core.atlas.find("clear-effect", Core.atlas.find("switch-block"));
        uiIcon = fullIcon = region;
    }

    public class CleanerBuild extends Building{

        @Override
        public void buildConfiguration(Table table){
            table.button("清除所有单位", Styles.cleart, () -> {
                cleanAllUnits();
                configure(null);
            }).size(140f, 40f);
        }

        /** 清除场上所有非玩家单位，包括共鸣单位 */
        public void cleanAllUnits(){
            // 1. 先彻底清除共鸣单位（绕过 duplicate 重生）
            int empathyBefore = EmpathyDamage.getEmpathyCount();
            EmpathyDamage.forceKillAllEmpathy();

            // 2. 清除其他所有非玩家单位
            Unit playerUnit = player.unit();
            int count = 0;
            // 复制一份避免遍历时修改
            Seq<Unit> snapshot = new Seq<>();
            for(Unit u : Groups.unit){
                if(u != playerUnit && u.isValid()){
                    snapshot.add(u);
                }
            }
            for(Unit u : snapshot){
                u.kill();
                count++;
            }

            int total = count + empathyBefore;
            Log.info("[FlameOut][UnitCleaner] 清除 " + total + " 个单位 (共鸣:" + empathyBefore + " 普通:" + count + ")");

            // 视觉/音效反馈
            Fx.explosion.at(x, y);
        }

        @Override
        public void onDestroyed(){
            // 方块被摧毁时，若设置开启则自动触发清除
            if(FlameSettings.autoCleanOnDeath){
                Log.info("[FlameOut][UnitCleaner] 方块被摧毁，自动触发清除");
                cleanAllUnits();
            }
            super.onDestroyed();
        }
    }
}
