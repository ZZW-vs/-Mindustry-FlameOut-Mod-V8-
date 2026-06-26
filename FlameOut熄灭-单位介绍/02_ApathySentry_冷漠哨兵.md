# ApathySentry（冷漠哨兵）- 单位分析报告

## 一、基本属性

**位置：** `src/flame/unit/ApathySentryUnit.java`  
**类型定义：** `src/flame/unit/FlameUnitTypes.java`

### 核心属性：
| 属性 | 数值 | 说明 |
|------|------|------|
| 生命值 | 9,000 | 9000点生命值 |
| 碰撞体积 | 10 | 10单位大小 |
| 拖拽系数 | 0.5 | 减速较快 |
| 武器 | 激光 | 激光武器 |
| 武器冷却 | 6帧 | 极快的射速 |

### 单位类型特征：
- **飞行单位**（flying = true）
- **隐藏单位**（hidden = true）- 不计入单位上限
- **无坠落效果**（fallEffect = Fx.none）
- **无法序列化**（serialize() = false）- 死亡后不保留状态

## 二、生成机制

### 触发条件：
ApathySentry 由 **Apathy** 单位在以下情况召唤：

1. **盾牌被破坏**
   ```java
   if(shieldStun > 0 || health < (maxHealth / 3f)){
       if(sentries.size < 8 && !createSentries){
           createSentries = true;
       }
   }
   ```

2. **生命值低于1/3**
   - 当 Apathy 的生命值降到最大值的1/3以下时
   - 自动开始召唤哨兵

3. **最多召唤8个哨兵**
   - 每次战斗最多召唤8个
   - 每个哨兵消耗一定资源

### 召唤过程：
```
1. createSentries = true
2. 计算哨兵位置（围绕Apathy成圈）
3. 每6帧生成一个哨兵
4. 哨兵位置：距离Apathy 200单位
5. 16个位置轮流生成
6. 生成16个后停止（实际最多8个）
```

## 三、行为模式

### 1. 治疗模式（Heal Mode）
**触发条件**：当 Apathy 的 `sentryHealTime > 0`

```
行为：
- 停止攻击
- 向Apathy移动
- 持续治疗Apathy
- 治疗量：maxHealth / 20 = 600,000点
- 治疗延迟：180帧 + index * 10帧
```

### 2. 攻击模式（Attack Mode）
**触发条件**：当没有治疗任务时

```
行为：
- 寻找最强目标（继承自Apathy）
- 追踪目标
- 发射激光攻击
- 每6帧重新加载武器
```

### 3. 重新定位模式（Reposition Mode）
**触发条件**：每3分钟或治疗完成后

```
行为：
- 移动到随机位置
- 距离Apathy：600单位
- 准备下一轮行动
```

## 四、武器系统

### 激光武器属性：
```java
武器名称：FlameBullets.sentryLaser
射程：基于武器定义
伤害：基于武器定义
射速：6帧一发
```

### 攻击逻辑：
```java
void update(){
    // 1. 检查目标是否有效
    if(Units.invalidateTarget(target, team, x, y, range)){
        target = null;
    }
    
    // 2. 瞄准目标
    if(target != null){
        rotation = Angles.moveToward(rotation, angleTo(target), 15f);
        
        // 3. 射击
        if(reload <= 0){
            FlameBullets.sentryLaser.create(this, position, rotation);
            reload = 6f;
        }
    }
}
```

## 五、与Apathy的协同

### 1. 继承目标系统
```java
// Apathy中
for(ApathySentryUnit s : sentries){
    s.target = ai.strongest;  // 继承最强目标
}

// 哨兵中
public void update(){
    // 使用继承的目标进行攻击
}
```

### 2. 伤害分担
```
Apathy受到的伤害会：
1. 首先扣除盾牌
2. 盾牌耗尽后伤害生命值
3. 生命值低时召唤哨兵
4. 哨兵可以治疗Apathy
```

### 3. 生命恢复
```java
// 治疗效果
owner.heal(owner.maxHealth / 20f);
// = 12000000 / 20 = 600,000点
```

## 六、特殊机制

### 1. 所有者引用
```java
ApathyIUnit owner;  // 引用Apathy单位
```

### 2. 移动系统
```java
void moveSentry(float wx, float wy){
    moveX = wx;
    moveY = wy;
    moveTime = 0f;
}

void update(){
    // 平滑移动到目标位置
    float lx = (moveX - x) * 0.25f * moveTime;
    float ly = (moveY - y) * 0.25f * moveTime;
    moveTime = Mathf.clamp(moveTime + Time.delta / 20f);
    x += lx;
    y += ly;
}
```

### 3. 治疗指示器
```java
// 在绘制时显示治疗线
if(healDelay > 0){
    Draw.color(Pal.heal);
    Lines.stroke(3f * healFade);
    Lines.line(x, y, owner.x, owner.y);
}
```

## 七、死亡机制

### 死亡条件：
```java
void update(){
    // 如果Apathy无效
    if(owner == null || !owner.isValid()){
        destroy();
        return;
    }
}
```

### 死亡效果：
```java
void destroy(){
    super.destroy();
    // 血液爆炸效果
    BloodSplatter.explosion(20, x, y, hitSize / 2, 80f, 35f);
}
```

## 八、关键代码片段

### 哨兵生成核心逻辑：
```java
Vec2 v = Tmp.v1.trns((360f / 16) * sentryPosition, 200f).add(x, y);

ApathySentryUnit s = (ApathySentryUnit)FlameUnitTypes.apathySentry.create(team);
s.set(v.x, v.y);
s.moveSentry(v.x, v.y);
s.rotation = (360f / 16) * sentryPosition;
s.active = false;
s.owner = this;  // 引用Apathy

s.add();
FlameFX.bigLaserFlash.at(s.x, s.y);
sentries.add(s);
```

### 治疗逻辑：
```java
if(healDelay > 0){
    rotation = Angles.moveToward(rotation, angleTo(owner), 15f);
    healDelay -= Time.delta;
    healFade = Mathf.clamp(healFade + Time.delta / 12f);
    if(healDelay <= 0f){
        owner.heal(owner.maxHealth / 20f);
        reload = 0f;
    }
}
```

## 九、战术价值

### 优势：
1. **持续治疗**：为Apathy恢复大量生命
2. **分担火力**：吸引敌方火力
3. **额外输出**：提供额外的激光伤害
4. **继承目标**：自动追踪最强敌人

### 弱点：
1. **生命值低**：只有9000点
2. **无护甲**：容易被集火击杀
3. **依赖Apathy**：Apathy死亡后自动消失
4. **移动迟缓**：需要时间到达位置

### 应对策略：
1. **优先击杀哨兵**：减少Apathy的治疗来源
2. **高DPS武器**：快速清理哨兵
3. **分割战场**：阻止哨兵接近Apathy
4. **破盾后集火**：利用Apathy无盾时的脆弱期
