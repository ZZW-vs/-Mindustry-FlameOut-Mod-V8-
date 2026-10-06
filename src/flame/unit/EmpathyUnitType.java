package flame.unit;

import flame.*;
import flame.effects.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class EmpathyUnitType extends UnitType{
    public EmpathyUnitType(String name){
        super(name);
        flying = true;
        hitSize = 7;
        drag = 0.07f;

        health = 100f;

        outlines = false;
        drawCell = false;
        bounded = false;

        createScorch = false;
        hidden = false;

        engineSize = -1f;

        // 使用 EmpathyUnit 的内置 AI，不使用 NullAI
        // controller = u -> new NullAI();

        envEnabled = Env.any;
        envDisabled = 0;
        constructor = EmpathyUnit::new;

        deathExplosionEffect = FlameFX.empathyDecoyDestroy;
        deathSound = FlameSounds.expDecoy;
    }

    @Override
    public void init(){
        super.init();
        for(StatusEffect s : Vars.content.statusEffects()){
            immunities.add(s);
        }
    }

    @Override
    public void load(){
        super.load();
        EmpathyRegions.load();
    }

    @Override
    public void update(Unit unit){
        super.update(unit);
    }

}
