package flame.special;

import arc.*;
import arc.graphics.g2d.*;
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
    static Table settingsButton;
    static boolean expanded = true;
    static final float buttonSize = 44f;
    static final float pad = 4f;

    static float posX = -1f;
    static float posY = -1f;

    public static boolean enabled = true;

    public static void load(){
        enabled = Core.settings.getBool(FlameSettings.keyMobileControls, true);
        posX = Core.settings.getFloat(FlameSettings.keyMobilePosX, -1f);
        posY = Core.settings.getFloat(FlameSettings.keyMobilePosY, -1f);
    }

    public static void savePos(){
        Core.settings.put(FlameSettings.keyMobilePosX, posX);
        Core.settings.put(FlameSettings.keyMobilePosY, posY);
    }

    public static void build(){
        if(buttonTable != null){
            buttonTable.remove();
            buttonTable = null;
        }

        if(!enabled || !mobile) return;

        //使用容器实现对齐定位，避免hudGroup布局覆盖setPosition的问题
        buttonTable = new Table(Tex.buttonTrans);
        buttonTable.touchable = Touchable.enabled;
        //设置布局对齐：右上角
        buttonTable.top().right();

        rebuild();

        buttonTable.pack();

        //计算初始位置：默认右上角
        float initX, initY;
        if(posX < 0 || posY < 0){
            //默认位置：屏幕右上角，避开顶部UI
            initX = Core.graphics.getWidth() - buttonTable.getWidth() - 10f;
            initY = Core.graphics.getHeight() - buttonTable.getHeight() - 80f;
        }else{
            initX = posX;
            initY = posY;
        }

        buttonTable.setPosition(initX, initY);
        clampPosition();

        buttonTable.addListener(new InputListener(){
            boolean dragging = false;
            float dragOffsetX, dragOffsetY;
            boolean moved;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                dragging = true;
                moved = false;
                //记录触摸点相对于按钮左下角的偏移
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
                    //移动按钮到新位置（保持触摸点相对偏移）
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

        //确保布局后位置正确
        buttonTable.update(() -> {
            //每帧检查位置是否被布局器覆盖，如果是则恢复
            if(posX >= 0 && posY >= 0){
                if(!Mathf.equal(buttonTable.x, posX, 1f) || !Mathf.equal(buttonTable.y, posY, 1f)){
                    buttonTable.setPosition(posX, posY);
                }
            }
        });

        buildSettingsButton();
    }

    //左下角齿轮按钮：快速打开FO设置面板，剧情中也能使用
    static void buildSettingsButton(){
        if(settingsButton != null){
            settingsButton.remove();
            settingsButton = null;
        }

        settingsButton = new Table();
        settingsButton.touchable = Touchable.enabled;
        settingsButton.bottom().left();

        settingsButton.button(Icon.settings, Styles.cleari, () -> {
            //直接显示FO设置对话框，不依赖游戏主菜单
            showFlameOutSettingsDialog();
        }).size(56f).pad(4f);

        //定位到屏幕左下角
        settingsButton.pack();
        settingsButton.setPosition(10f, 10f);

        ui.hudGroup.addChild(settingsButton);

        //每帧校正位置（防止hudGroup布局覆盖）
        settingsButton.update(() -> {
            settingsButton.visible = state.isGame() && enabled;
            if(!Mathf.equal(settingsButton.x, 10f, 1f) || !Mathf.equal(settingsButton.y, 10f, 1f)){
                settingsButton.setPosition(10f, 10f);
            }
        });
    }

    //独立的FO设置对话框，剧情中也能打开
    static mindustry.ui.dialogs.BaseDialog flameOutSettingsDialog;
    static void showFlameOutSettingsDialog(){
        if(flameOutSettingsDialog == null){
            flameOutSettingsDialog = new mindustry.ui.dialogs.BaseDialog("FlameOut 设置");
            flameOutSettingsDialog.addCloseButton();
            flameOutSettingsDialog.cont.margin(14f);
            FlameSettings.buildAll(flameOutSettingsDialog.cont);
        }
        flameOutSettingsDialog.show();
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

        //重新计算位置（尺寸变化后）
        if(posX >= 0 && posY >= 0){
            buttonTable.setPosition(posX, posY);
            clampPosition();
        }
    }

    static void simulateReset(){
        //手机版虚拟按键不检查disableStoryKeys（该标志仅为禁用键盘快捷键而设）
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
        if(settingsButton != null){
            settingsButton.remove();
            settingsButton = null;
        }
    }
}
