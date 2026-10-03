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

    @Override
    public void update(){
        updateValues();
        updatePlayerUltimate();
        super.update();
    }

    /**
     * 玩家操控时的按键映射（仅影响被玩家操控之后的表现）。
     * <p>
     * 电脑端把"开火"从鼠标左键改到鼠标右键，把左键留给大招；
     * 未被玩家操控时（AI 操控）本方法不会被调用，因此不影响原版表现；
     * 手机端沿用游戏自带的开火按钮，保持默认。
     */
    @Override
    public void controlWeapons(boolean rotate, boolean shoot){
        boolean controlled = isPlayer() || (Vars.player != null && Vars.player.unit() == this);
        if(controlled && !Vars.mobile){
            boolean normal = FlameControl.normalAttackHeld();
            super.controlWeapons(normal, normal);
        }else{
            super.controlWeapons(rotate, shoot);
        }
    }

    /**
     * 玩家操控时的大招驱动逻辑。
     * <p>
     * 原版大招由 {@link DespondencyAI} 控制：它会设置主武器的 shoot/target，并累加 activeTime。
     * 玩家接管后 AI 不再运行，因此这里补上同样的一段驱动：
     * 玩家按下鼠标左键（或手机「技」按钮）-> 选择最强敌方单位 -> 打开大招开关，武器自身状态机接管后续流程。
     */
    private void updatePlayerUltimate(){
        //isPlayer() 依赖 controller instanceof Player；再用 Vars.player.unit() 兜底，
        //避免极少数情况下控制器判定不一致导致大招逻辑完全不执行。
        boolean controlled = isPlayer() || (Vars.player != null && Vars.player.unit() == this);
        if(!controlled){
            playerUltimate = false;
            return;
        }

        if(ultimateCooldown > 0f) ultimateCooldown -= Time.delta;

        DespondencyUnitType type = (DespondencyUnitType)this.type;
        WeaponMount main = mounts[type.mainWeaponIdx];

        boolean held = FlameControl.ultimateHeld();

        if(playerUltimate){
            //持续把目标刷新为当前最强的敌方单位，避免目标死亡后大招中断
            Unit best = findUltimateTarget();
            if(best != null) main.target = best;

            if(main.target instanceof Unit u && u.isValid() && !u.dead()){
                //大招演出期间自动面向目标，方便进入下一阶段
                rotation = Angles.moveToward(rotation, angleTo(u), 3f * Time.delta);
            }
        }else if(held && ultimateCooldown <= 0f){
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
        double bestScore = -Double.MAX_VALUE;
        //直接遍历全场景单位，避免依赖 state.teams.present 的填充时机。
        for(Unit u : Groups.unit){
            if(u == this || u.team == team || u.team == Team.derelict) continue;
            if(!u.isValid() || u.dead) continue;
            double s = ((double)FlameOutSFX.inst.getUnitDps(u.type)) + (double)(u.maxHealth * u.healthMultiplier) - u.dst(this) / 1000f;
            if(best == null || s > bestScore){
                best = u;
                bestScore = s;
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
