package flame.entities;

import mindustry.content.*;
import mindustry.gen.*;

public class MockBullet extends Bullet{
    public MockBullet(){
        //v159: Bullet.update() accesses type fields (accel, drag etc.), ensure type is never null to prevent NPE
        type = Bullets.placeholder;
    }

    @Override
    public void add(){}

    @Override
    public void remove(){}
}
