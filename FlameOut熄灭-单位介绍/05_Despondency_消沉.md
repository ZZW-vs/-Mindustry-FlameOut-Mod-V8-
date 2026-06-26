# Despondency（消沉）- 单位分析报告

## 一、基本属性

**位置：** `src/flame/unit/DespondencyUnitType.java`  
**主要实现：** `src/flame/unit/DespondencyUnit.java`  
**AI系统：** `src/flame/unit/DespondencyAI.java`

### 核心属性：
| 属性 | 数值 | 说明 |
|------|------|------|
| 生命值 | 17,500,000 | 1750万生命值 |
| 碰撞体积 | 217 | 超大体积 |
| 移动速度 | 2 | 缓慢移动 |
| 旋转速度 | 0.5 | 非常缓慢旋转 |
| 护甲 | 200 | 极高护甲值 |
| 攻击范围 | 720 | 720单位射程 |

### 单位类型特征：
- **飞行单位**（flying = true）
- **悬停模式**（hovering = true）
- **低空飞行**（lowAltitude = true）
- **锁定腿部基座**（lockLegBase = true）
- **不创建焦痕**
- **无法序列化**
- **所有状态免疫**
- **无法沉没**（canDrown = false）

## 二、腿部系统

### 1. 腿部参数
```java
legForwardScl = 0.75f;
legLength = 672f;           // 672单位长度
legExtension = -48f;          // 腿部延伸
legCount = 8;                // 8条腿
legGroupSize = 2;            // 每组2条腿
legPairOffset = 1f;          // 腿部对偏移
legMoveSpace = 0.33f;        // 移动空间
legBaseOffset = 51.25f / 4f;  // 基座偏移
legLengthScl = 0.9f;         // 腿部长度缩放
baseLegStraightness = 1f;    // 基座直度
legStraightness = 0.01f;     // 腿部直度
legStraightLength = 4f;       // 直线长度
legSplashRange = 100f;        // 溅射范围
legSplashDamage = 5400f;     // 溅射伤害
```

### 2. 腿部布局
```
8条腿分布在身体两侧：
- 左4条，右4条
- 每2条一组，共4组
- 腿部长度672单位
- 覆盖范围极广
```

## 三、武器系统（6种武器）

### 1. 防空武器（Anti-Air）
```java
new EndAntiAirWeapon(this.name + "-anti-air"){{
    x = 45.75f;
    y = 15.5f;
    mirror = true;
    useAmmo = false;
    reload = 3f * 60f;  // 3秒冷却
    bullet = new BulletType(0f, 9000f);  // 9000伤害
}}
```
**用途**：攻击飞行单位

### 2. 激光炮（Laser）
```java
new LaserWeapon(this.name + "-laser"){{
    x = 54.5f;
    y = -15f;
    mirror = true;
    continuous = true;       // 持续激光
    rotate = true;          // 旋转
    reload = 8f * 60f;      // 8秒
    rotationLimit = 190f;    // 旋转限制
    rotateSpeed = 3.5f;      // 旋转速度
    bullet = new EndCreepLaserBulletType();
}}
```
**用途**：持续激光伤害

### 3. 电磁炮（Railgun）
```java
new Weapon(this.name + "-railgun"){{
    x = 63f;
    y = -34f;
    shootY = 12f;
    mirror = true;
    rotate = true;
    reload = 110f;          // 110帧
    rotateSpeed = 1.6f;
    bullet = new EndRailBulletType();
}}
```
**用途**：高伤害穿透射击

### 4. 核弹炮（Cannon）
```java
new Weapon(this.name + "-cannon"){{
    x = 56f;
    y = -62.75f;
    shootY = 12f;
    mirror = true;
    rotate = true;
    reload = 8.5f * 60f * 2f;  // 17秒
    bullet = new EndNukeBulletType();  // 核弹！
}}
```
**用途**：范围大爆炸

### 5. 专属武器（Special）
```java
new EndDespondencyWeapon()
```
**用途**：主要攻击手段，最强武器

### 6. 导弹发射器（Missile）
```java
new EndLauncherWeapon(this.name + "-missile")
```
**用途**：多发导弹攻击

## 四、AI系统详解

### 1. 目标系统
```java
public void updateTargeting(){
    // 评估所有敌人
    for(TeamData data : Vars.state.teams.present){
        // 评估单位
        for(Unit u : data.units){
            double s = FlameOutSFX.inst.getUnitDps(u.type) + 
                       u.maxHealth * u.healthMultiplier - 
                       u.dst(unit) / 1000f;
            targets.add(u, s);
        }
        
        // 评估建筑
        for(Building build : data.buildings){
            float s = build.maxHealth;
            // 加权计算...
            targets.add(build, s);
        }
    }
}
```

### 2. 优先目标选择
```java
// 最高威胁目标
target = highestScoreUnit;

// 移动优先目标
if(!altTarget) moveTarget = highestDPSUnit;
```

### 3. 移动控制
```java
@Override
public void updateMovement(){
    Teamc move = moveTarget ?? target;
    
    if(move != null && deathTime < 5f * 60f){
        vec.set(move).sub(unit).limit(unit.speed() * (death ? 0.5f : 1f));
        
        if(!move.within(unit, near)){
            unit.movePref(vec);  // 接近
        }else if(move.within(unit, near / 2f)){
            unit.move(-vec.x, -vec.y);  // 后退
        }
    }
}
```

## 五、终结技系统（Death System）

### 1. 触发条件
```java
if(target instanceof Unit u && 
   !death && 
   !EmpathyDamage.containsExclude(u.id) && 
   unit.within(u, unit.range() * 0.8f + u.hitSize / 2.5f) && 
   reloadTime <= 0f){
    
    float scr = FlameOutSFX.inst.getUnitDps(u.type) + 
                u.maxHealth * u.healthMultiplier;
    
    if(EmpathyDamage.isNaNInfinite(scr) || 
       scr > 1000000f || 
       (u.hitSize > 100f && scr > 200000f)){
        death = true;  // 触发终结技！
    }
}
```

### 2. 终结技过程
```java
// 1. 停止所有武器
for(WeaponMount mount : unit.mounts){
    mount.shoot = false;
}

// 2. 瞄准目标
main.shoot = true;
main.target = target;

// 3. 充能
if(target != null) main.target = target;
deathTime += Time.delta;

if(deathTime >= 30f){
    dm.activeTime += Time.delta;
}

// 4. 释放
// EndDespondencyWeapon执行终结技
```

### 3. 终结技效果
```java
// 释放后
deathTime = 0f;
reloadTime = 10f * 60f;  // 10秒冷却
death = false;
```

## 六、真实生命值系统

### 1. 生命值独立追踪
```java
class DespondencyUnit extends LegsUnit{
    float trueHealth, trueMaxHealth;
    float invFrames;
    float lastDamage = 0f;
}
```

### 2. 伤害限制
```java
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
```

### 3. 免疫排除
```java
@Override
public void add(){
    if(!added){
        trueHealth = type.health;
        EmpathyDamage.exclude(this);  // 排除在伤害追踪外
    }
    super.add();
}
```

## 七、武器更新系统

### 1. 武器分配逻辑
```java
@Override
public void updateWeapons(){
    DespondencyUnitType type = (DespondencyUnitType)unit.type;
    WeaponMount main = unit.mounts[type.mainWeaponIdx];
    
    // 分配目标给各武器
    for(WeaponMount mount : unit.mounts){
        if(mount == main) continue;
        
        Teamc tar = targets.get(idx % targets.size);
        mount.target = tar;
        mount.shoot = shouldShoot();
    }
    
    // 主武器特殊处理
    if(death){
        main.shoot = true;
        main.target = target;
    }
}
```

### 2. 目标优先级
```java
// 优先攻击高威胁目标
double s = FlameOutSFX.inst.getUnitDps(u.type) + 
           u.maxHealth * u.healthMultiplier;

// 大型单位优先
if(u.hitSize > 100f){
    s *= 2f;
}
```

## 八、关键代码片段

### 目标评估：
```java
double s = (((double)FlameOutSFX.inst.getUnitDps(u.type)) + 
            (double)(u.maxHealth * u.healthMultiplier)) - 
            (u.dst(unit) / 1000f);

if(t == null || s > score){
    t = u;
    score = s;
}
```

### 终结技判定：
```java
float scr = FlameOutSFX.inst.getUnitDps(u.type) + 
            u.maxHealth * u.healthMultiplier;

if(EmpathyDamage.isNaNInfinite(scr) || 
   scr > 1000000f || 
   (u.hitSize > 100f && scr > 200000f)){
    death = true;
}
```

### 伤害限制：
```java
amount = Math.min(amount, type.health / 220f);
// = 17500000 / 220 = 79545.45
// 每次最多受到约8万伤害
```

## 九、战术价值

### 优势：
1. **生命值极高**：1750万生命
2. **护甲极高**：200护甲值
3. **多种武器**：适应不同情况
4. **终结技**：对高威胁目标致命
5. **范围攻击**：AOE能力强

### 弱点：
1. **速度慢**：容易被风筝
2. **转向慢**：难以追踪快速目标
3. **冷却长**：终结技需要时间
4. **位置固定**：近身后难以逃脱

### 应对策略：
1. **高速单位**：风筝战术
2. **高护甲穿透**：减少护甲影响
3. **集火输出**：快速击杀
4. **破甲武器**：降低防御
5. **持续压制**：不给他释放终结技的机会

## 十、与Yggdrasil的对比

| 属性 | Despondency | Yggdrasil |
|------|-------------|-----------|
| 生命值 | 17,500,000 | 2,250,000 |
| 碰撞体积 | 217 | 25 |
| 移动速度 | 2 | 5 |
| 护甲 | 200 | 20 |
| 武器数量 | 6种 | 触手射击 |
| 终结技 | 有 | 无 |

### 总结：
- **Despondency**：重装堡垒，适合正面对抗
- **Yggdrasil**：快速骚扰，适合灵活作战
