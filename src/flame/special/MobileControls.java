package flame.special;

import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.math.*;
import flame.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MobileControls{
    public static Table buttonTable;
    static boolean expanded = true;
    static final float buttonSize = 44f;
    static final float pad = 4f;

    static float posX = -1f;
    static float posY = -1f;

    public static boolean enabled = false;

    public static void load(){
        enabled = Core.settings.getBool(FlameSettings.keyMobileControls, false);
        posX = Core.settings.getFloat(FlameSettings.keyMobilePosX, -1f);
        posY = Core.settings.getFloat(FlameSettings.keyMobilePosY, -1f);
    }

    public static void savePos(){
        Core.settings.put(FlameSettings.keyMobilePosX, posX);
        Core.settings.put(FlameSettings.keyMobilePosY, posY);
    }

    public static void build(){
        //先清理旧的面板
        if(buttonTable != null){
            buttonTable.remove();
            buttonTable = null;
        }

        //仅在启用且手机平台时创建
        if(!enabled || !mobile) return;

        buttonTable = new Table(Tex.buttonTrans);
        buttonTable.touchable = Touchable.enabled;

        rebuild();

        buttonTable.pack();

        //计算初始位置：默认右上角
        float initX, initY;
        if(posX < 0 || posY < 0){
            initX = Core.graphics.getWidth() - buttonTable.getWidth() - 10f;
            initY = Core.graphics.getHeight() - buttonTable.getHeight() - 80f;
        }else{
            initX = posX;
            initY = posY;
        }

        buttonTable.setPosition(initX, initY);
        clampPosition();

        //拖拽监听
        buttonTable.addListener(new InputListener(){
            boolean dragging = false;
            float dragOffsetX, dragOffsetY;
            boolean moved;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                dragging = true;
                moved = false;
                dragOffsetX = x;
                dragOffsetY = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(dragging){
                    if(Math.abs(x - dragOffsetX) > 2f || Math.abs(y - dragOffsetY) > 2f){
                        moved = true;
                    }
                    float newX = buttonTable.x + (x - dragOffsetX);
                    float newY = buttonTable.y + (y - dragOffsetY);
                    buttonTable.setPosition(newX, newY);
                    clampPosition();
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(dragging){
                    dragging = false;
                    posX = buttonTable.x;
                    posY = buttonTable.y;
                    savePos();
                    if(moved){
                        event.stop();
                    }
                }
            }
        });

        ui.hudGroup.addChild(buttonTable);

        //每帧校正位置，防止 hudGroup 布局器覆盖 setPosition
        buttonTable.update(() -> {
            if(posX >= 0 && posY >= 0){
                if(!Mathf.equal(buttonTable.x, posX, 1f) || !Mathf.equal(buttonTable.y, posY, 1f)){
                    buttonTable.setPosition(posX, posY);
                }
            }
        });
    }

    static void clampPosition(){
        float x = buttonTable.x;
        float y = buttonTable.y;
        float w = buttonTable.getWidth();
        float h = buttonTable.getHeight();
        float sw = Core.graphics.getWidth();
        float sh = Core.graphics.getHeight();

        if(x < 0f) x = 0f;
        if(y < 0f) y = 0f;
        if(x + w > sw) x = sw - w;
        if(y + h > sh) y = sh - h;

        buttonTable.setPosition(x, y);
    }

    static void rebuild(){
        buttonTable.clearChildren();
        buttonTable.top().left();
        buttonTable.margin(pad);

        Table content = new Table();
        content.top().left();

        if(expanded){
            //第1行：设置 + 贴图菜单
            content.button(Icon.settings, Styles.cleari, () -> {
                FlameSettings.showSettings();
            }).size(buttonSize).pad(2f);

            content.button(Icon.tree, Styles.cleari, () -> {
                if(SecretSpritesMenu.dialog == null) SecretSpritesMenu.load();
                SecretSpritesMenu.rebuild();
                SecretSpritesMenu.dialog.show();
            }).size(buttonSize).pad(2f);

            content.row();

            //第2行：Z(重置) + X(前进)
            content.table(t -> {
                t.button("Z", Styles.flatt, () -> simulateReset()).size(buttonSize).pad(2f);
                t.button("X", Styles.flatt, () -> simulateNext()).size(buttonSize).pad(2f);
            });

            content.row();

            //第3行：V(启动) + C(退出)
            content.table(t -> {
                t.button("V", Styles.flatt, () -> simulateStart()).size(buttonSize).pad(2f);
                t.button("C", Styles.flatt, () -> simulateQuit()).size(buttonSize).pad(2f);
            });

            content.row();

            //第4行：B(快进) + 收起按钮
            content.table(t -> {
                Button ffBtn = new Button(Styles.flatToggleMenut){
                    {
                        update(() -> {
                            if(isPressed() && !FlameSettings.disableStory){
                                simulateFastForward();
                            }
                        });
                    }
                };
                ffBtn.add("B").get().setFontScale(0.8f);
                t.add(ffBtn).size(buttonSize).pad(2f);

                t.button(Icon.right, Styles.cleari, () -> {
                    expanded = false;
                    rebuild();
                }).size(buttonSize).pad(2f);
            });
        }else{
            //收起状态：只显示展开按钮
            content.button(Icon.left, Styles.cleari, () -> {
                expanded = true;
                rebuild();
            }).size(buttonSize).pad(2f);
        }

        buttonTable.add(content);
        buttonTable.pack();

        //尺寸变化后重新校正位置
        if(posX >= 0 && posY >= 0){
            buttonTable.setPosition(posX, posY);
            clampPosition();
        }
    }

    static void simulateReset(){
        if(FlameSettings.disableStory) return;
        Log.info("[FlameOut][Mobile] 重置阶段 -> 0");
        SpecialMain.state = 0;
        Core.settings.put("flame-special", 0);
        SpecialMain.activeState = null;
    }

    static void simulateNext(){
        if(FlameSettings.disableStory) return;
        if(SpecialMain.state < 5){
            SpecialMain.increment(false);
            Log.info("[FlameOut][Mobile] 前进到下一阶段 -> " + SpecialMain.state);
        }
    }

    static void simulateStart(){
        if(FlameSettings.disableStory) return;
        Log.info("[FlameOut][Mobile] 启动剧情");
        if(SpecialMain.state == 0){
            SpecialMain.state = 1;
            Core.settings.put("flame-special", SpecialMain.state);
        }
        if(SpecialMain.state >= 1 && SpecialMain.state <= 5){
            SpecialMain.loadState();
            if(SpecialMain.activeState != null){
                SpecialMain.activeState.loadAssets();
                SpecialMain.activeState.loadClient();
            }
        }
    }

    static void simulateQuit(){
        if(FlameSettings.disableStory) return;
        Log.info("[FlameOut][Mobile] 退出剧情");
        SpecialMain.state = 0;
        Core.settings.put("flame-special", 0);
        SpecialMain.activeState = null;
        Core.app.exit();
    }

    static void simulateFastForward(){
        if(FlameSettings.disableStory) return;
        if(SpecialMain.activeState != null){
            try{
                java.lang.reflect.Field f = SpecialMain.activeState.getClass().getDeclaredField("time");
                f.setAccessible(true);
                float t = (float)f.get(SpecialMain.activeState);
                f.set(SpecialMain.activeState, t + 30f * 60f);
            }catch(Exception ignored){}
        }
    }

    public static void update(){
        //需要显示但尚未创建时自动创建
        if(buttonTable == null && enabled && mobile && state.isGame()){
            build();
        }
        if(buttonTable != null){
            buttonTable.visible = state.isGame() && enabled;
        }
    }

    public static void rebuildIfNeeded(){
        if(buttonTable != null){
            rebuild();
        }
    }

    public static void dispose(){
        if(buttonTable != null){
            buttonTable.remove();
            buttonTable = null;
        }
    }
}
