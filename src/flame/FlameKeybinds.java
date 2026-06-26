package flame;

import arc.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class FlameKeybinds{
    public static final String[] keys = {
        "key-reset",
        "key-next",
        "key-start",
        "key-quit",
        "key-fastforward",
        "key-sprites"
    };

    public static final String[] keyNames = {
        "重置剧情",
        "下一阶段",
        "启动剧情",
        "退出剧情",
        "快进(按住)",
        "隐藏贴图菜单"
    };

    public static final KeyCode[] defaults = {
        KeyCode.z,
        KeyCode.x,
        KeyCode.v,
        KeyCode.c,
        KeyCode.b,
        KeyCode.h
    };

    static ObjectMap<String, KeyCode> keyMap = new ObjectMap<>();
    static BaseDialog dialog;

    public static void load(){
        for(int i = 0; i < keys.length; i++){
            String name = keys[i];
            String saved = Core.settings.getString(name, null);
            if(saved != null){
                try{
                    keyMap.put(name, KeyCode.valueOf(saved));
                    continue;
                }catch(Exception ignored){}
            }
            keyMap.put(name, defaults[i]);
        }

        Events.on(ClientLoadEvent.class, e -> {
            Vars.ui.settings.addCategory("FlameOut", t -> {
                t.margin(14f);
                rebuildTable(t);
            });
        });
    }

    public static KeyCode get(String name){
        KeyCode k = keyMap.get(name);
        return k == null ? KeyCode.unset : k;
    }

    public static void set(String name, KeyCode code){
        keyMap.put(name, code);
        Core.settings.put(name, code.name());
    }

    public static boolean tap(String name){
        return Core.input.keyTap(get(name));
    }

    public static boolean down(String name){
        return Core.input.keyDown(get(name));
    }

    static void rebuildTable(Table t){
        t.clearChildren();
        for(int i = 0; i < keys.length; i++){
            String key = keys[i];
            String label = keyNames[i];
            KeyCode current = get(key);

            t.add(label).left().padRight(20f);
            Button b = t.button(current.toString(), Styles.flatt, () -> {
                BaseDialog d = new BaseDialog("设置按键");
                d.cont.add("按下任意键设置\n按 ESC 取消").pad(30f);
                d.keyDown(k -> {
                    if(k == KeyCode.escape){
                        d.hide();
                        return;
                    }
                    set(key, k);
                    d.hide();
                    rebuildTable(t);
                });
                d.addCloseButton();
                d.show();
            }).width(180f).get();
            t.row();
        }

        t.button("恢复默认", Styles.flatt, () -> {
            for(int i = 0; i < keys.length; i++){
                set(keys[i], defaults[i]);
            }
            rebuildTable(t);
        }).colspan(2).fillX().padTop(10f);
    }
}
