package flame.unit;

import arc.math.*;
import arc.util.*;
import flame.*;
import flame.unit.empathy.*;
import flame.unit.weapons.EndDespondencyWeapon.*;
import mindustry.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;

public class DespondencyUnit extends LegsUnit{
    float trueHealth, trueMaxHealth;
    float invFrames;
    float lastDamage = 0f;

    /** 玩家操控时，当前是否正在释放"终末"大招。 */
    public boolean playerUltimate = false;
    /** 玩家大招冷却计时（秒*60）。 */
    public float ultimateCooldown = 0f;
    /** 上一帧技能键是否按住（用于"刚按下"边沿检测）。 */
    private boolean prevUltimateHeld = false;

    @Override
    public void update(){
        updateValues();
        updatePlayerUltimate();
        super.update();
    }

    /**
     * 玩家操控时的大招驱动逻辑。
     * <p>
     * 原版大招由 {@link DespondencyAI} 控制：它会设置主武器的 shoot/target，并累加 activeTime。
     * 玩家接管后 AI 不再运行，因此这里补上同样的一段驱动：
     * 玩家按下技能键 -> 选择最强敌方单位 -> 打开大招开关，武器自身状态机接管后续流程。
     */
    private void updatePlayerUltimate(){
        if(!isPlayer()){
            playerUltimate = false;
            prevUltimateHeld = false;
            return;
        }

        if(ultimateCooldown > 0f) ultimateCooldown -= Time.delta;

        DespondencyUnitType type = (DespondencyUnitType)this.type;
        WeaponMount main = mounts[type.mainWeaponIdx];

        boolean held = FlameControl.attackHeld();
        boolean tap = held && !prevUltimateHeld;
        prevUltimateHeld = held;

        if(playerUltimate){
            //持续把目标刷新为当前最强的敌方单位，避免目标死亡后大招中断
            Unit best = findUltimateTarget();
            if(best != null) main.target = best;

            if(main.target instanceof Unit u && u.isValid() && !u.dead()){
                //大招演出期间自动面向目标，方便进入下一阶段
                rotation = Angles.moveToward(rotation, angleTo(u), 3f * Time.delta);
            }
        }else if(tap && ultimateCooldown <= 0f){
            Unit best = findUltimateTarget();
            if(best != null){
                playerUltimate = true;
                main.target = best;
                rotation = angleTo(best);
            }
        }
    }

    /** 大招结束回调（由 EndDespondencyWeapon 在阶段结束/失败时调用）。 */
    public void endPlayerUltimate(boolean success){
        playerUltimate = false;
        ultimateCooldown = success ? 10f * 60f : 2f * 60f;
        DespondencyUnitType type = (DespondencyUnitType)this.type;
        mounts[type.mainWeaponIdx].target = null;
    }

    /** 选取当前最强的敌方单位作为大招目标（与 AI 的打分方式保持一致）。 */
    private Unit findUltimateTarget(){
        Unit best = null;
        double score = -Double.MAX_VALUE;
        for(TeamData data : Vars.state.teams.present){
            if(data.team == team || data.team == Team.derelict) continue;
            for(Unit u : data.units){
                if(!u.isValid() || u.dead) continue;
                double s = ((double)FlameOutSFX.inst.getUnitDps(u.type)) + (double)(u.maxHealth * u.healthMultiplier) - u.dst(this) / 1000f;
                if(best == null || s > score){
                    best = u;
                    score = s;
                }
            }
        }
        return best;
    }

    @Override
    public void rawDamage(float amount){
        if(EmpathyDamage.isNaNInfinite(amount)) return;
        if(invFrames <= 0f || amount > lastDamage){
            float lam = amount;

            amount -= lastDamage;
            lastDamage = lam;
            amount = Math.min(amount, type.health / 220f);
            trueHealth -= amount;
            super.rawDamage(amount);
            trueHealth = health;
            invFrames = 15f;
        }
    }

    @Override
    public boolean isGrounded(){
        return true;
    }

    void updateValues(){
        if(!EmpathyDamage.isNaNInfinite(health)) trueHealth = Math.max(trueHealth, health);

        health = trueHealth;
        maxHealth = trueMaxHealth;
        if(trueHealth > 0){
            elevation = 1f;
            dead = false;
        }else{
            elevation = 0f;
            dead = true;
        }
        if(invFrames > 0){
            invFrames -= Time.delta;
            if(invFrames <= 0f){
                lastDamage = 0f;
            }
        }
    }

    @Override
    public void setType(UnitType type){
        super.setType(type);
        trueMaxHealth = type.health;
    }

    @Override
    public SolidPred solidity(){
        return null;
    }

    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public void add(){
        if(!added){
            trueHealth = type.health;
            EmpathyDamage.exclude(this);
        }
        super.add();
    }

    @Override
    public void remove(){
        if(trueHealth > 0 && EmpathyDamage.containsExclude(id)) return;
        if(added){
            boolean valid = EmpathyDamage.removeExclude(this);
            if(valid){
                super.remove();
            }else{
                trueHealth = trueMaxHealth = type.health;
            }
        }
    }
}
