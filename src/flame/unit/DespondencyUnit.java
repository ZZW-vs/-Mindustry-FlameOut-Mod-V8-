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
     * 玩家操控时的大招驱动逻辑。
     * <p>
     * 普通攻击走原版开火逻辑（鼠标左键），无需在此处理；
     * 大招由鼠标右键（或手机「技」按钮）触发：
     * <ul>
     *     <li>按下大招键即进入大招状态，<b>没有目标也会开始并持续蓄力</b>，
     *         只有出现目标后武器状态机才会真正推进大招阶段。</li>
     *     <li>大招进行中不再每帧自动索敌，只有当当前目标失效（死亡/无效）时才重新寻找目标，
     *         避免"玩家一按大招就被自动索敌牵着走"。</li>
     * </ul>
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
            //大招进行中：不再每帧自动索敌。
            //只有当前目标失效（死亡/销毁/无效）时才重新寻找一个目标，
            //这样玩家按下大招后不会被自动索敌牵着转向；目标出现后大招自然开始攻击。
            boolean validTarget = main.target instanceof Unit u && u.isValid() && !u.dead();
            if(!validTarget){
                Unit best = findUltimateTarget();
                if(best != null) main.target = best;
            }

            if(main.target instanceof Unit u && u.isValid() && !u.dead()){
                //大招演出期间自动面向目标，方便进入下一阶段
                rotation = Angles.moveToward(rotation, angleTo(u), 3f * Time.delta);
            }
        }else if(held && ultimateCooldown <= 0f){
            //按下大招键即进入大招状态：没有目标也要开始（此时只会持续蓄力，
            //武器状态机在 EndDespondencyWeapon 中仅在存在目标时才推进阶段）。
            playerUltimate = true;
            Unit best = findUltimateTarget();
            if(best != null){
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
