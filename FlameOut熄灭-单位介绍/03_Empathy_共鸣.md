# Empathy（共鸣）- 单位分析报告

## 一、基本属性

**位置：** `src/flame/unit/EmpathyUnitType.java`  
**主要实现：** `src/flame/unit/empathy/EmpathyUnit.java`  
**AI系统：** `src/flame/unit/empathy/EmpathyAI.java`  
**伤害系统：** `src/flame/unit/empathy/EmpathyDamage.java`

### 核心属性：
| 属性 | 数值 | 说明 |
|------|------|------|
| 生命值 | 100 | 显示值，仅受微量伤害 |
| 真实生命值 | 独立追踪 | 完全独立的生命系统 |
| 碰撞体积 | 7 | 7单位大小 |
| 拖拽系数 | 0.07 | 较灵活的移动 |
| 飞行高度 | 1.0 | 完全飞行 |

### 单位类型特征：
- **飞行单位**（flying = true）
- **免疫所有状态效果**（所有状态免疫）
- **无法序列化**（不保留状态）
- **无坠落效果**
- **不创建焦痕**
- **隐藏单位**（hidden = true）
- **Boss单位**

## 二、生命值系统

### 1. 双层生命系统
Empathy 拥有**完全独立的**双层生命系统：

```
显示生命值（假的）：
- 初始：100
- 每次伤害：最多 100/500 = 0.2 点
- 实际不掉血

真实生命值（真的）：
- 独立变量 trueHealth
- 正常受到所有伤害
- 用于判定真正死亡
```

### 2. 伤害限制机制
```java
// 每次伤害被大幅限制
float cdamage = Mathf.clamp(amount, 0f, 100f / 500f);
// = Mathf.clamp(amount, 0f, 0.2f)

// 实际伤害计算
trueHealth -= cdamage;
health -= cdamage;  // 显示值也减少
invFrames = 30f;     // 30帧无敌帧
```

### 3. 无敌帧系统
```java
float invFrames;  // 无敌帧计数器

void trueDamage(float amount){
    if(invFrames <= 0f){
        // 只在无敌帧结束后才能受伤
        trueHealth -= amount;
    }
}
```

## 三、复活/复制机制

### 1. 复制触发条件
```java
void updateDamageTaken(float amount){
    damageTaken += amount;
    maxDamageTaken = Math.max(maxDamageTaken, damageTaken);
    damageDelay = 60f;
    
    // 触发条件：
    // 1. decoyDelay <= 0（不在诱饵状态）
    // 2. damageTaken >= 700,000 或 受到无限/NaN伤害
    if(decoyDelay <= 0f && 
       (damageTaken >= 700000f || EmpathyDamage.isNaNInfinite(damageTaken, maxDamageTaken))){
        duplicate();  // 触发复制！
    }
}
```

### 2. 复制过程详解
```java
EmpathyUnit duplicate(){
    // 1. 创建传输数据
    EmpathyTransferData o = new EmpathyTransferData();
    o.d = this.d;      // 复制所有状态数据
    o.dcy = this.dcy;  // 复制诱饵状态
    o.t = this.team;    // 复制队伍
    
    // 2. 设置诱饵延迟（2分钟）
    this.decoyDelay = 2f * 60;
    
    // 3. 创建新Empathy
    EmpathyUnit u1 = createUnit(o);
    u1.setType(type);
    u1.ammo = type.ammoCapacity;
    u1.elevation = 1f;
    u1.copyFields(this);  // 复制所有字段
    u1.team = team;
    
    // 4. 当前单位变成诱饵
    this.decoy = true;
    this.health = this.maxHealth = 100000000f;  // 诱饵有1亿血
    this.attackAIs.clear();
    this.movementAIs.clear();
    this.initDecoyAIs();  // 初始化诱饵AI
    
    // 5. 新单位加入战场
    u1.add();
    EmpathyDamage.onDuplicate(this, u1);
    
    return u1;
}
```

### 3. 诱饵系统（Decoy）
```
诱饵特性：
- 生命值：100,000,000（1亿）
- 只能使用简化AI
- 5分钟后死亡
- 吸引火力
- 无法真正造成伤害
```

### 4. 伤害衰减系统
```java
void updateDamageTaken(){
    if(damageDelay <= 0f){
        if(damageTaken > 0 && maxDamageTaken > 0){
            // 每5分钟衰减一次
            damageTaken = Math.max(0f, damageTaken - (maxDamageTaken / (5f * 60f)));
            if(damageTaken <= 0f){
                maxDamageTaken = 0f;
            }
        }
    }else{
        damageDelay -= Time.delta;
    }
}
```

## 四、移动AI系统

### 1. 环绕移动（OrbitMove）
- 环绕目标移动
- 保持一定距离
- 可能改变距离和角度

### 2. 随机传送（RandomTeleport）
- 随机传送到地图位置
- 可能传送到目标附近
- 可能传送到远处

### 3. 位置交换（TeleSwapMove）
- 与目标交换位置
- 瞬间移动
- 战术价值极高

## 五、攻击AI系统（18种攻击模式）

### 基础攻击模式：

#### 1. PinAttack（插刺攻击）
- 向目标发射尖刺
- 尖刺插在地上造成持续伤害
- 适合控制区域

#### 2. SprayAttack（弹幕射击）
- 向四周发射弹幕
- 多个方向同时攻击
- 适合对付多个目标

#### 3. SwordBarrageAttack（剑气乱舞）
- 发射多道剑气
- 剑气轨迹随机
- 伤害范围大

#### 4. LaserShotgunAttack（激光霰弹）
- 发射多束激光
- 激光快速扩散
- 中距离攻击

#### 5. RicochetAttack（反弹攻击）
- 子弹可以反弹
- 反弹多次
- 难以躲避

#### 6. ShineAttack（闪耀攻击）
- 发光特效
- 可能造成范围伤害
- 视觉效果震撼

### 高级攻击模式：

#### 7. MagicAttack（魔法攻击）
- 使用魔法弹幕
- 可能附带特殊效果
- 多样化的攻击方式

#### 8. DepowerAttack（削弱攻击）
- 降低目标能力
- 可能减少敌人伤害
- 削弱效果持久

#### 9. RendAttack（撕裂攻击）
- 造成持续伤害
- 撕裂效果
- 难以治愈

#### 10. PrimeAttack（主要攻击）
- 最常用的攻击模式
- 平衡伤害和频率
- AI最常选择

#### 11. DashAttack（冲刺攻击）
- 快速接近目标
- 冲刺过程中无敌
- 冲刺后攻击

#### 12. CopyAttack（复制攻击）⭐NB
- 复制玩家的武器！
- 使用玩家的武器攻击
- 完全复制武器属性

#### 13. BlackHoleAttack（黑洞攻击）⭐NB
- 创建黑洞效果
- 吸引周围单位
- 造成持续伤害

#### 14. SwordAttack（剑攻击）
- 实体剑攻击
- 近距离高伤害
- 快速挥剑

#### 15. HandAttack（手攻击）
- 用手攻击
- 可能是抓取效果
- 特殊动画

#### 16. BlastAttack（爆炸攻击）
- 范围爆炸
- 高伤害
- 爆炸特效

#### 17. CountDownAttack（倒计时攻击）⭐NB
```java
// 数5个数字
// 然后大爆炸！
// 极难阻止
count = 5;
void update(){
    if(count > 0){
        count--;
        // 显示倒计时
    }else{
        // 大爆炸！
    }
}
```

#### 18. EndAttack（终结技）⭐最强
- 最强攻击模式
- 组合多种攻击
- 致命效果

#### 19. SurroundLaserAttack（环绕激光）
- 激光环绕
- 360度攻击
- 无法躲避

## 六、格挡系统（Parry System）

### 1. 自动格挡
```java
void updateAutoParry(){
    // 扫描锥形区域
    Utils.scanCone(..., 25f, 60f, b -> {
        if(b.team != unit.team){
            parryFound = true;
        }
    });
    
    // 发现敌人时自动格挡
    if(parryFound){
        autoParryTime = 40f;
        unit.parry();
    }
}
```

### 2. 格挡效果
```java
void parry(){
    // 1. 反弹锥形区域内所有子弹
    Utils.scanCone(..., 35f, 80f, b -> {
        b.team = unit.team;  // 改变队伍
        b.vel.add(dx, dy);  // 加速弹射
    });
    
    // 2. 反弹导弹和定时爆炸单位
    Utils.scanCone(..., 35f, 80f, u -> {
        if(u instanceof MissileAI){
            ((MissileAI)u.controller()).shooter = null;
        }
        u.team = unit.team;
    });
    
    // 3. 恢复生命值到格挡开始时
    parryS();  // trueHealth = Math.max(trueHealth, parryHealth);
    
    // 4. 触发特效和音效
    FlameFX.empathyParry.at(x, y, rotation);
    FlameSounds.empathyParry.at(x, y);
}
```

### 3. 激光捕获
```java
// 捕获特定类型的子弹
if(b.type instanceof LaserBulletType || 
   b.type instanceof RailBulletType || 
   b.type instanceof ShrapnelBulletType){
    // 存储子弹用于反击
    parryLasers.add(h);
}
```

## 七、AI智能选择系统

### 1. 权重系统
```java
void randAI(boolean attack, boolean quickSwap){
    randAI.clear();
    for(EmpathyAI ai : attackAIs){
        float use = 1f + (ai.aiUsages / 2f);
        randAI.add(ai, ai.weight() / use);
    }
    EmpathyAI i = randAI.get();
    // 根据权重随机选择AI
}
```

### 2. 学习型AI
```java
// 记录AI使用次数
ai.aiUsages++;

// 使用次数越多，被选中的概率越低
float use = 1f + (ai.aiUsages / 2f);
randAI.add(ai, ai.weight() / use);
```

### 3. 攻击性判断
```java
boolean useLethal(){
    return targetLowHealth() ||           // 目标血量低
           battleTime > 4f * 60f * 60f || // 战斗超过4小时
           nearestTotalHealth >= 2000000 ||  // 总血量高
           (attackAIChanges % 7) >= 6;      // 连续使用次数多
}

float extraLethalScore(){
    return 10f / Math.max(0.000001f, getTargetHealthFract());
}
```

## 八、绝对伤害系统（AbsoluteDamage）

### 1. 伤害追踪
```java
// 追踪每个敌人的真实生命值
damageMap: IntMap<AbsoluteDamage<?>>
damages: Seq<AbsoluteDamage<?>>

void damageUnit(Unit unit, float damage, boolean lethal, Runnable onDeath){
    // 创建或更新伤害记录
    AbsoluteDamage<?> ad = damageMap.get(unit.id);
    ad.damage(damage, lethal, onDeath);
}
```

### 2. 多次复活机制
```java
// 敌人死亡后：
// 1. 第一次：从Groups.all移除
// 2. 第二次：重新添加
// 3. 第三次：Empathy复制自己
// 4. 第四次：再次复制
// 5. 第五次及以后：彻底清除（purgatory）

for(EmpathyHolder u : units){
    if(!u.added){
        u.removeCount++;
        switch(u.removeCount){
            case 1, 2 -> u.unit.add();  // 复活
            case 3, 4 -> {
                EmpathyUnit en = u.unit.duplicate();  // 复制
                empathyMap.remove(u.unit.id);
                empathyMap.put(en.id, u);
                u.unit = en;
            }
        }
    }
}
```

### 3. NaN检测与修复
```java
public static boolean isNaNInfinite(float ...fields){
    for(float v : fields){
        if(Float.isNaN(v) || Float.isInfinite(v) || v >= Float.MAX_VALUE) return true;
    }
    return false;
}

// 如果发现NaN，立即复制并锁定
void nanLock(Unit unit, float x, float y){
    if(ad instanceof UnitAbsoluteDamage ud){
        ud.nanTime = 5f;
        ud.nanX = x;
        ud.nanY = y;
    }
}
```

## 九、跨地图重生机制

### 1. EmpathySpawner
```java
public static void spawnEmpathy(float x, float y){
    EmpathySpawner s = new EmpathySpawner();
    s.x = x;
    s.y = y;
    s.active = true;
    s.shouldSpawn = true;
    spawner = s;
}
```

### 2. 世界加载时重生
```java
public static void worldLoad(){
    if(spawner != null && spawner.shouldSpawn){
        spawner.reactivateTime = 5f * 60f;  // 5分钟后
        spawner.timeScl++;
        spawner.shouldSpawn = false;
    }
}
```

### 3. 状态保存
```java
// 保存生命值和倒计时
spawner.health = u.getTrueHealth();
spawner.countDown = u.getCountDown();
```

## 十、关键代码片段

### 伤害处理核心：
```java
void trueDamage(float amount){
    if(!isDecoy()){
        updateDamageTaken(Math.max(0f, amount));
    }
    if(EmpathyDamage.isNaNInfinite(amount)) amount = 0f;
    if(!isDecoy()){
        if(invFrames <= 0f){
            float cdamage = Mathf.clamp(amount, 0f, 100f / 500f);
            if(parryTime <= 0f){
                parryHealth = trueHealth;
                parryTime = 6f;
            }
            trueHealth -= cdamage;
            health -= cdamage;
            invFrames = 30f;
            hitTime = 1.0f;
        }
    }
}
```

### 格挡核心：
```java
void parry(){
    Utils.scanCone((QuadTree<Bullet>)Groups.bullet.tree(), 
                   x, y, rotation, 35f, 80f, b -> {
        b.rotation(angleTo(b));
        float dx = ((b.x - x) / 20f) * 2.25f;
        float dy = ((b.y - y) / 20f) * 2.25f;
        b.vel.add(dx, dy);
        b.team = team;  // 反弹！
    });
    parryS();  // 恢复生命
}
```

## 十一、高级NB特性总结

### 1. **完全独立的生命系统**
- 显示生命和真实生命完全分离
- 伤害被大幅限制
- 无敌帧保护

### 2. **多层复活机制**
- 受伤超过70万自动复制
- 敌人最多复活4次
- 诱饵系统吸引火力

### 3. **智能AI选择**
- 根据使用频率调整权重
- 根据目标状态选择攻击
- 持续学习优化

### 4. **绝对伤害追踪**
- 独立追踪所有敌人
- 即使被删除也能复活
- 防止作弊

### 5. **完美格挡系统**
- 自动反弹所有子弹
- 改变敌人队伍
- 恢复自身生命

### 6. **跨维度能力**
- 检测并修复NaN值
- 位置锁定机制
- 跨地图重生

## 十二、彻底清除Empathy的方法

要完全清除Empathy，必须：

1. **清除所有EmpathyUnit**（包括诱饵）
2. **重置EmpathyDamage**（清空所有追踪数据）
3. **禁用EmpathySpawner**（阻止跨地图重生）
4. **清除所有状态**（防止任何复活）

```java
// 单位清除器必须执行：
EmpathyDamage.reset();  // 重置所有数据
// 然后从所有组中移除单位
Groups.all.remove(unit);
Groups.unit.remove(unit);
unit.dead = true;
unit.health = 0;
```
