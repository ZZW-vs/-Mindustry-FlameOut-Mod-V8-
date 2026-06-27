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
import flame.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MobileControls{
    public static Table buttonTable;
    static boolean expanded = true;
    static final float buttonSize = 48f;
    static final float pad = 6f;

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

        buttonTable = new Table(Tex.buttonTrans);
        buttonTable.touchable = Touchable.enabled;

        rebuild();

        if(posX < 0 || posY < 0){
            buttonTable.setPosition(
                Core.graphics.getWidth() - buttonTable.getWidth() - 10f,
                Core.graphics.getHeight() - buttonTable.getHeight() - 120f
            );
        }else{
            buttonTable.setPosition(posX, posY);
        }

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
                    buttonTable.moveBy(x - dragOffsetX, y - dragOffsetY);
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
            Button btn1 = content.button(Icon.settings, Styles.cleari, () -> {
                FlameSettings.showDialog();
            }).size(buttonSize).pad(2f).get();

            Button btn2 = content.button(Icon.tree, Styles.cleari, () -> {
                if(SecretSpritesMenu.dialog == null) SecretSpritesMenu.load();
                SecretSpritesMenu.rebuild();
                SecretSpritesMenu.dialog.show();
            }).size(buttonSize).pad(2f).get();

            content.row();

            Button btnZ = new Button(Styles.flatt);
            btnZ.add("Z").get().setFontScale(0.9f);
            btnZ.clicked(() -> simulateReset());
            content.add(btnZ).size(buttonSize).pad(2f);

            Button btnX = new Button(Styles.flatt);
            btnX.add("X").get().setFontScale(0.9f);
            btnX.clicked(() -> simulateNext());
            content.add(btnX).size(buttonSize).pad(2f);

            content.row();

            Button btnV = new Button(Styles.flatt);
            btnV.add("V").get().setFontScale(0.9f);
            btnV.clicked(() -> simulateStart());
            content.add(btnV).size(buttonSize).pad(2f);

            Button btnC = new Button(Styles.flatt);
            btnC.add("C").get().setFontScale(0.9f);
            btnC.clicked(() -> simulateQuit());
            content.add(btnC).size(buttonSize).pad(2f);

            content.row();

            Button ffBtn = new Button(Styles.flatToggleMenut){
                {
                    update(() -> {
                        if(isPressed() && !FlameSettings.disableStory && !FlameSettings.disableStoryKeys){
                            simulateFastForward();
                        }
                    });
                }
            };
            ffBtn.add("B").get().setFontScale(0.8f);
            content.add(ffBtn).size(buttonSize).pad(2f);

            Button btnHide = content.button(Icon.right, Styles.cleari, () -> {
                expanded = false;
                rebuild();
            }).size(buttonSize).pad(2f).get();
        }else{
            Button btnShow = content.button(Icon.left, Styles.cleari, () -> {
                expanded = true;
                rebuild();
            }).size(buttonSize).pad(2f).get();
        }

        buttonTable.add(content);
        buttonTable.pack();
    }

    static void simulateReset(){
        if(FlameSettings.disableStoryKeys || FlameSettings.disableStory) return;
        Log.info("[FlameOut][Mobile] 重置阶段 -> 0");
        SpecialMain.state = 0;
        Core.settings.put("flame-special", 0);
        SpecialMain.activeState = null;
    }

    static void simulateNext(){
        if(FlameSettings.disableStoryKeys || FlameSettings.disableStory) return;
        if(SpecialMain.state < 5){
            SpecialMain.increment(false);
            Log.info("[FlameOut][Mobile] 前进到下一阶段 -> " + SpecialMain.state);
        }
    }

    static void simulateStart(){
        if(FlameSettings.disableStoryKeys || FlameSettings.disableStory) return;
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
        if(FlameSettings.disableStoryKeys || FlameSettings.disableStory) return;
        Log.info("[FlameOut][Mobile] 退出剧情");
        SpecialMain.state = 0;
        Core.settings.put("flame-special", 0);
        SpecialMain.activeState = null;
        Core.app.exit();
    }

    static void simulateFastForward(){
        if(FlameSettings.disableStoryKeys || FlameSettings.disableStory) return;
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
    }
}
