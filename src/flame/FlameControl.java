package flame;

import arc.*;
import arc.input.*;
import mindustry.*;

/**
 * 跨平台技能输入中枢。
 * <p>
 * Boss 单位在被玩家操控时，原版 AI 会被 Player 控制器替换掉，导致无法释放技能。
 * 本类统一收集"技能键"的按住状态：
 * <ul>
 *     <li>电脑端：鼠标左键/右键。</li>
 *     <li>手机端：读取 {@link flame.special.MobileControls} 虚拟按键设置的触摸状态。</li>
 * </ul>
 * 各 Boss 单位在自己的 update() 中读取下列方法，并自行做"刚按下"边沿检测，
 * 因此不依赖 FlameControl 的调用时机（避免帧序问题）。
 */
public class FlameControl{
    /** 手机虚拟按键：技能键是否被按住（对应「技」按钮）。 */
    private static boolean mobileAttack = false;
    /** 手机虚拟按键：瞬移键是否被按住（对应「瞬」按钮）。 */
    private static boolean mobileMove = false;

    /** 手机虚拟按键回调：设置技能键状态。 */
    public static void setMobileAttack(boolean down){
        mobileAttack = down;
    }

    /** 手机虚拟按键回调：设置瞬移键状态。 */
    public static void setMobileMove(boolean down){
        mobileMove = down;
    }

    /**
     * 当前瞬移是否来自手机虚拟按键。
     * <p>
     * 手机端沿用原版 AI 的随机瞬移方式；电脑端则瞬移到鼠标位置。
     */
    public static boolean isMobileTeleport(){
        return mobileMove;
    }

    /**
     * 共鸣随机攻击键：电脑鼠标左键 或 手机「技」按钮。
     * <p>
     * 同时也可以让共鸣单位按左键时的普通射击逻辑照常工作（共鸣本身无武器，不冲突）。
     */
    public static boolean attackHeld(){
        return mobileAttack || keyDown(KeyCode.mouseLeft);
    }

    /**
     * 共鸣瞬移键：电脑鼠标右键 或 手机「瞬」按钮。
     */
    public static boolean teleportHeld(){
        return mobileMove || keyDown(KeyCode.mouseRight);
    }

    /**
     * 消沉大招键：电脑鼠标左键 或 手机「技」按钮。
     */
    public static boolean ultimateHeld(){
        return mobileAttack || keyDown(KeyCode.mouseLeft);
    }

    /**
     * 消沉普通攻击键：电脑鼠标右键。
     * <p>
     * 手机端仍然沿用游戏自带的开火按钮，不在这里处理。
     */
    public static boolean normalAttackHeld(){
        return keyDown(KeyCode.mouseRight);
    }

    /** 读取键盘按键状态，非游戏中返回 false。 */
    private static boolean keyDown(KeyCode code){
        if(Vars.headless || Core.input == null || !Vars.state.isGame()) return false;
        return Core.input.keyDown(code);
    }

    /** 清理所有输入状态（退出游戏/关闭面板时调用，防止残留）。 */
    public static void reset(){
        mobileAttack = false;
        mobileMove = false;
    }
}