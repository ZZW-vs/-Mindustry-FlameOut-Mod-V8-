package flame;

import arc.*;
import mindustry.*;

/**
 * 跨平台技能输入中枢。
 * <p>
 * Boss 单位在被玩家操控时，原版 AI 会被 Player 控制器替换掉，导致无法释放技能。
 * 本类统一收集"技能键"的按住状态：
 * <ul>
 *     <li>电脑端：读取 {@link FlameKeybinds} 中配置的键盘按键（默认 F / G）。</li>
 *     <li>手机端：读取 {@link flame.special.MobileControls} 虚拟按键设置的触摸状态。</li>
 * </ul>
 * 各 Boss 单位在自己的 update() 中读取 {@link #attackHeld()} / {@link #moveHeld()}，
 * 并自行做"刚按下"边沿检测，因此不依赖 FlameControl 的调用时机（避免帧序问题）。
 */
public class FlameControl{
    /** 手机虚拟按键：技能攻击键是否被按住。 */
    private static boolean mobileAttack = false;
    /** 手机虚拟按键：瞬移/移动技能键是否被按住。 */
    private static boolean mobileMove = false;

    /** 手机虚拟按键回调：设置技能攻击键状态。 */
    public static void setMobileAttack(boolean down){
        mobileAttack = down;
    }

    /** 手机虚拟按键回调：设置瞬移键状态。 */
    public static void setMobileMove(boolean down){
        mobileMove = down;
    }

    /**
     * 技能攻击键是否按住（键盘 或 手机虚拟按键）。
     * <p>
     * 用于触发各 Boss 的主动技能（如 despondency 的大招、empathy 的随机攻击）。
     */
    public static boolean attackHeld(){
        if(mobileAttack) return true;
        if(Vars.headless || Core.input == null || !Vars.state.isGame()) return false;
        return Core.input.keyDown(FlameKeybinds.get("key-skill-attack"));
    }

    /**
     * 瞬移/移动技能键是否按住（键盘 或 手机虚拟按键）。
     * <p>
     * 用于触发瞬移类技能。
     */
    public static boolean moveHeld(){
        if(mobileMove) return true;
        if(Vars.headless || Core.input == null || !Vars.state.isGame()) return false;
        return Core.input.keyDown(FlameKeybinds.get("key-skill-move"));
    }

    /** 清理所有输入状态（退出游戏/关闭面板时调用，防止残留）。 */
    public static void reset(){
        mobileAttack = false;
        mobileMove = false;
    }
}