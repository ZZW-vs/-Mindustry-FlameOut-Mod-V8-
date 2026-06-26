package flame.special;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.special.states.*;
import mindustry.*;

public class SpecialMain{
    public static ObjectMap<String, TextureRegion> regions = new ObjectMap<>();
    public static Seq<TextureRegion> regionSeq = new Seq<>();
    static int offset = 691038;
    static Texture main;
    static SpecialState activeState;

    private static int state = 0;
    private static float logTimer = 0f;   // 周期性状态报告的计时器

    public static void draw(){
        if(activeState != null){
            activeState.draw();
        }
        SecretSpritesMenu.draw();
    }

    public static void update(){
        updateTest();

        if(activeState != null){
            activeState.update();
        }

        SecretSpritesMenu.update();

        // 每 2 秒输出一次当前状态，方便调试
        logTimer += Time.delta;
        if(logTimer >= 120f){
            logTimer = 0f;
            float curTime = -1f;
            try{
                if(activeState != null){
                    java.lang.reflect.Field f = activeState.getClass().getDeclaredField("time");
                    f.setAccessible(true);
                    curTime = (float)f.get(activeState);
                }
            }catch(Exception ignored){}
            Log.info("[FlameOut][Status] state=" + state +
                ", activeState=" + (activeState == null ? "null" : activeState.getClass().getSimpleName()) +
                ", time=" + (curTime < 0 ? "N/A" : (int)(curTime / 60f) + "s") +
                ", mainTex=" + (main != null ? "loaded" : "fallback") +
                ", regionSeq=" + regionSeq.size);
        }
    }

    public static void updateTest(){
        if(FlameKeybinds.tap("key-reset")){
            Log.info("[FlameOut][Key-Reset] 重置阶段 -> 0");
            state = 0;
            Core.settings.put("flame-special", state);
            activeState = null;
            Log.info("[FlameOut][Key-Reset] 完成: activeState=null, saved to settings");
        }

        if(FlameKeybinds.tap("key-next")){
            if(state < 5){
                increment(false);
                Log.info("[FlameOut][Key-Next] 前进到下一阶段 -> " + state);
            } else {
                Log.info("[FlameOut][Key-Next] 已达最大阶段 5，无法继续");
            }
        }

        if(FlameKeybinds.tap("key-start")){
            Log.info("[FlameOut][Key-Start] ==================== 启动剧情 ====================");
            Log.info("[FlameOut][Key-Start] 当前 state = " + state);
            if(state == 0){
                state = 1;
                Core.settings.put("flame-special", state);
                Log.info("[FlameOut][Key-Start] state 为 0，强制设置为 1（Stage1）");
            }
            if(state >= 1 && state <= 5){
                Log.info("[FlameOut][Key-Start] 调用 loadState(" + state + ") ...");
                loadState();
                if(activeState != null){
                    Log.info("[FlameOut][Key-Start]  activeState = " + activeState.getClass().getSimpleName());
                    Log.info("[FlameOut][Key-Start]  调用 loadAssets() ...");
                    activeState.loadAssets();
                    Log.info("[FlameOut][Key-Start]  调用 loadClient() ...");
                    activeState.loadClient();
                    Log.info("[FlameOut][Key-Start]  剧情 Stage " + state + " 已启动！请查看菜单变化");
                } else {
                    Log.err("[FlameOut][Key-Start]  ERROR: loadState() 后 activeState 仍然为 null！");
                }
            } else {
                Log.info("[FlameOut][Key-Start] state=" + state + " > 5，请先重置");
            }
            Log.info("[FlameOut][Key-Start] ======================================================");
        }

        if(FlameKeybinds.tap("key-quit")){
            Log.info("[FlameOut][Key-Quit] ==================== 退出剧情 ====================");
            Log.info("[FlameOut][Key-Quit] 重置 state 为 0 并重启游戏");
            state = 0;
            Core.settings.put("flame-special", 0);
            activeState = null;
            Core.app.exit();
        }

        if(FlameKeybinds.down("key-fastforward") && activeState != null){
            try{
                java.lang.reflect.Field f = activeState.getClass().getDeclaredField("time");
                f.setAccessible(true);
                float t = (float)f.get(activeState);
                f.set(activeState, t + 30f * 60f);
                // 每秒输出一次快进报告
                if((int)(t / 60f) % 2 == 0 && (int)(t / 60f) != (int)((t + 30f * 60f) / 60f) / 2){
                    Log.info("[FlameOut][Key-B] 快进中: 当前 time=" + (int)(t / 60f) + "s -> " + (int)((t + 30f * 60f) / 60f) + "s");
                }
            } catch(Exception ignored){}
        }
    }

    public static void increment(){
        increment(true);
    }

    public static void increment(boolean change){
        if(state >= 6){
            return;
        }
        state++;
        Core.settings.put("flame-special", state);
        Log.info("[FlameOut][increment] state -> " + state + ", change=" + change);
        if(change) loadState();
    }

    public static int getStage(){
        return state;
    }

    public static boolean validEmpathySpawn(){
        return state == 0 || state >= 5;
    }

    public static void dispose(){
        if(main != null) main.dispose();
        activeState = null;
    }

    public static void load(){
        Log.info("[FlameOut] ==================================================");
        Log.info("[FlameOut]  FlameOut mod loaded");
        Log.info("[FlameOut]  剧情快捷键:");
        Log.info("[FlameOut]    [" + FlameKeybinds.get("key-reset") + "] 重置剧情到阶段 0");
        Log.info("[FlameOut]    [" + FlameKeybinds.get("key-next") + "] 前进到下一阶段");
        Log.info("[FlameOut]    [" + FlameKeybinds.get("key-start") + "] 启动剧情");
        Log.info("[FlameOut]    [" + FlameKeybinds.get("key-quit") + "] 退出剧情并重启游戏");
        Log.info("[FlameOut]    [" + FlameKeybinds.get("key-fastforward") + "] 按住快进，跳过等待阶段");
        Log.info("[FlameOut]    [" + FlameKeybinds.get("key-sprites") + "] 隐藏贴图菜单");
        Log.info("[FlameOut]  (可在游戏设置 -> FlameOut 中修改快捷键)");
        Log.info("[FlameOut] ==================================================");

        state = Core.settings.getInt("flame-special", 0);
        Log.info("[FlameOut][load] 初始化，从 settings 读到 state = " + state);

        if(state > 5){
            Log.info("[FlameOut][load] state > 5，跳过剧情加载");
            return;
        }

        try{
            Fi file = Vars.tree.get("extras/Vultures.png");
            byte[] bytes = file.readBytes();
            Pixmap map = new Pixmap(bytes, offset, bytes.length);
            main = new Texture(map);
            main.setFilter(TextureFilter.linear);

            loadRegion("main", 0, 0, 1023, 1023);
            loadRegion("ball", 1024, 192, 1247, 319);
            loadRegion("hug", 1024, 320, 1407, 639);
            loadRegion("cat", 1408, 256, 1791, 639);
            loadRegion("bunny0", 1792, 512, 1919, 639);
            loadRegion("bunny1", 1024, 640, 1279, 895);
            loadRegion("bunny2", 1280, 640, 1535, 895);
            loadRegion("bunny3", 1536, 640, 1791, 895);
            loadRegion("bunny4", 1792, 640, 2047, 895);
            loadRegion("flower", 1026, 896, 1533, 1023);
            loadRegion("tree", 1536, 896, 2047, 1023);

            Log.info("[FlameOut][load] 特殊纹理资源加载成功，regionSeq.size=" + regionSeq.size);
        }catch(Exception e){
            main = null;
            Log.info("[FlameOut][load] 未找到特殊纹理 extras/Vultures.png，使用 fallback 占位纹理");

            TextureRegion placeholder = Core.atlas.find("clear-effect");
            for(int i = 0; i < 12; i++){
                regions.put("ph" + i, placeholder);
                regionSeq.add(placeholder);
            }
        }

        loadState();

        if(activeState != null){
            Log.info("[FlameOut][load] 自动加载阶段 activeState=" + activeState.getClass().getSimpleName());
            activeState.loadAssets();
        } else {
            Log.info("[FlameOut][load] 未自动加载任何阶段（state=" + state + "，按 V 手动启动）");
        }
    }

    public static void loadClient(){
        Log.info("[FlameOut][loadClient] 被调用，activeState=" + (activeState == null ? "null" : activeState.getClass().getSimpleName()));

        if(activeState == null) loadState();

        if(activeState != null){
            Log.info("[FlameOut][loadClient] 调用 activeState.loadClient()");
            activeState.loadClient();
        } else {
            Log.info("[FlameOut][loadClient] 没有活跃的阶段，跳过 loadClient");
        }
    }

    static void loadRegion(String name, int u, int v, int u2, int v2){
        Texture tex = main;

        TextureRegion reg = new TextureRegion(tex, u, v, (u2 + 1) - u, (v2 + 1) - v);

        if(name.equals("bunny3")){
            reg.width = (int)(reg.width * 0.75f);
            reg.height = (int)(reg.height * 0.75f);
        }
        if(name.equals("bunny4")){
            reg.width = (int)(reg.width * 1.5f);
            reg.height = (int)(reg.height * 1.5f);
        }
        if(name.equals("ball")){
            reg.width = (int)(reg.width * 1.25f);
            reg.height = (int)(reg.height * 1.25f);
        }

        regions.put(name, reg);
        regionSeq.add(reg);
    }

    static void loadState(){
        Log.info("[FlameOut][loadState] 切换到 state=" + state);
        activeState = null;
        switch(state){
            case 1 -> activeState = new Stage1();
            case 2 -> activeState = new Stage2();
            case 3 -> activeState = new Stage3();
            case 4 -> activeState = new Stage4();
            case 5 -> activeState = new Stage5();
            default -> Log.info("[FlameOut][loadState] state=" + state + " 不在 1-5 范围内，跳过");
        }

        if(activeState != null){
            Log.info("[FlameOut][loadState] 创建 " + activeState.getClass().getSimpleName() + "，调用 init()");
            activeState.init();
        }
    }
}
