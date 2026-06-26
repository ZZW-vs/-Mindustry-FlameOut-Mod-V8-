# Yggdrasil（世界树）- 单位分析报告

## 一、基本属性

**位置：** `src/flame/unit/YggdrasilUnitType.java`  
**主要实现：** `src/flame/unit/YggdrasilUnit.java`  
**AI系统：** `src/flame/unit/YggdrasilAI.java`

### 核心属性：
| 属性 | 数值 | 说明 |
|------|------|------|
| 生命值 | 2,250,000 | 225万生命值 |
| 碰撞体积 | 25 | 25单位大小 |
| 移动速度 | 5 | 较快的移动速度 |
| 旋转速度 | 2 | 缓慢旋转 |
| 护甲 | 20 | 中等护甲值 |
| 攻击范围 | 720 | 720单位射程 |

### 单位类型特征：
- **地面单位**（hovering = true）
- **允许腿部行走**（allowLegStep = true）
- **不绘制单元格**（drawCell = false）
- **自定义路径成本**（pathCost = ControlPathfinder.costLegs）
- **免疫所有状态效果**（除燃烧和融化）
- **不创建焦痕**

## 二、肢体系统

### 1. 腿部系统（YggdrasilLeg）

#### 腿部数量：32条腿
```java
for(int i = 0; i < 32; i++){
    YggdrasilLeg leg = new YggdrasilLeg();
    leg.set(this);
    legs.add(leg);
}
```

#### 腿部分组：4组
```java
for(int i = 0; i < groupSize; i++){
    for(int j = i; j < legs.size; j += groupSize){
        YggdrasilLeg l = legs.get(j);
        l.group = i;
    }
}
```

#### 腿部运动逻辑：
```
1. 将32条腿分成4组
2. 每次只有一组腿在移动
3. 其他组保持支撑
4. 4组轮流移动
5. 平滑过渡
```

### 2. 触手系统（YggdrasilTentacle）

#### 触手数量：24条触手
```java
for(int i = 0; i < 24; i++){
    YggdrasilTentacle t = new YggdrasilTentacle();
    t.set(this, Mathf.random(360f));
    tentacles.add(t);
}
```

#### 触手攻击：
```
射击过程：
1. 寻找目标位置
2. 触手依次射击
3. 射击间隔：140帧 / 24 ≈ 5.8帧
4. 循环射击
```

## 三、AI系统

### 1. 目标系统
```java
@Override
public void updateTargeting(){
    // 找到主要目标
    target = findMainTarget(unit.x, unit.y, unit.range() * 2, true, true);
    
    // 预测拦截点
    Vec2 to = Predict.intercept(unit.x, unit.y, target.x(), target.y(), 
                                 dx, dy, 20f);
    unit.aimX = to.x;
    unit.aimY = to.y;
}
```

### 2. 移动系统
```java
@Override
public void updateMovement(){
    // 如果目标是核心，保持距离
    if(!(target instanceof CoreBuild) && target != null){
        move = false;  // 不移动，专注于射击
    }
    
    // 射线检测移动路径
    World.raycastEachWorld(unit.x, unit.y, target.x(), target.y(), 
                          (x, y) -> {
        // 检测路径是否可通行
    });
    
    // 根据距离调整移动
    if(canMoveToward && !unit.within(target, unit.range())){
        // 接近目标
        unit.move(mv);
    }else if(canMoveToward && near){
        // 后退保持距离
        unit.move(-mv.x, -mv.y);
    }
}
```

### 3. 面部朝向
```java
@Override
public void faceTarget(){
    if(target != null){
        unit.lookAt(target);
    }else if(unit.moving()){
        unit.lookAt(unit.vel().angle());
    }
}
```

## 四、腿部运动详解

### 1. 组运动逻辑
```java
void update(){
    float maxProg = 0f;
    int cgroup = Mathf.mod(group, groupSize);  // 当前组
    
    // 找出最大进度
    for(YggdrasilLeg leg : legs){
        if(leg.group != cgroup){
            maxProg = Math.max(maxProg, leg.getProgress(this));
        }
    }
    
    // 平滑进度
    smoothProgress = Math.max(Mathf.lerpDelta(smoothProgress, 
                           Math.min(maxProg, 2f), 0.25f), smoothProgress);
}
```

### 2. IK逆运动学
```java
// 每条腿使用IK系统
void updateIK(Unit unit){
    // 计算关节位置
    // 更新腿部姿态
    // 保持平衡
}
```

### 3. 运动周期
```
1. group++：切换到下一组
2. smoothProgress = 0：重置进度
3. 当前组开始移动
4. 其他组提供支撑
5. 重复
```

## 五、触手攻击系统

### 1. 射击逻辑
```java
void update(){
    // 更新触手目标
    for(YggdrasilTentacle t : tentacles){
        t.updateTargetPosition(aimX, aimY);
        t.update(this);
    }
    
    // 射击
    if(isShooting){
        YggdrasilTentacle t = tentacles.get(tentacleIdx);
        if(tentacleReload <= 0f && t.canShoot()){
            t.shoot(aimX, aimY);
            tentacleIdx = (tentacleIdx + 1) % tentacles.size;
            tentacleReload = 140f / tentacles.size + 3f;
        }
    }
    tentacleReload -= Time.delta;
}
```

### 2. 轮流射击
```
24条触手轮流射击：
- 每条约5.8帧射击一次
- 循环不断
- 总射速：140帧完成一圈 + 3帧间隔
```

## 六、绘制系统

### 1. 绘制层级
```java
void draw(){
    // 1. 绘制阴影（最底层）
    Draw.z(z - 0.02f);
    type.applyColor(this);
    
    // 2. 绘制腿部
    for(YggdrasilLeg leg : legs){
        leg.draw(this);
    }
    
    // 3. 绘制触手（中间层）
    Draw.z(z);
    for(YggdrasilTentacle t : tentacles){
        t.draw(this);
    }
    
    // 4. 绘制主体（最上层）
    super.draw();
}
```

### 2. 贴图资源
```java
// 腿部贴图（3种）
legRegions[0-2]
legBaseRegions[0-2]

// 触手贴图（3种 + 结尾）
tentacleRegions[0-2]
tentacleEndRegion
```

## 七、与Despondency的对比

| 属性 | Yggdrasil | Despondency |
|------|-----------|-------------|
| 生命值 | 2,250,000 | 17,500,000 |
| 碰撞体积 | 25 | 217 |
| 移动速度 | 5 | 2 |
| 护甲 | 20 | 200 |
| 武器类型 | 触手射击 | 多种武器 |
| 攻击范围 | 720 | 720 |

### Yggdrasil的优势：
1. **速度更快**：移动速度是Despondency的2.5倍
2. **更灵活**：32条腿可以快速调整位置
3. **骚扰能力强**：触手持续射击

### Yggdrasil的弱点：
1. **生命值低**：只有Despondency的13%
2. **护甲低**：只有Despondency的10%
3. **单一体攻击**：只有一个主要攻击方式

## 八、关键代码片段

### 触手更新：
```java
void update(){
    // 更新触手
    for(YggdrasilTentacle t : tentacles){
        t.updateTargetPosition(aimX, aimY);
        t.update(this);
    }
    
    // 轮流射击
    if(isShooting){
        YggdrasilTentacle t = tentacles.get(tentacleIdx);
        if(tentacleReload <= 0f && t.canShoot()){
            t.shoot(aimX, aimY);
            tentacleIdx = (tentacleIdx + 1) % tentacles.size;
            tentacleReload = 140f / tentacles.size + 3f;
        }
    }
}
```

### 腿部IK：
```java
// 每条腿使用逆运动学
void updateIK(Unit unit){
    // 计算目标位置
    // 计算关节角度
    // 应用约束
    // 更新位置
}
```

## 九、战术价值

### 优势：
1. **骚扰能力强**：触手持续不断射击
2. **移动灵活**：可以快速调整位置
3. **适应性强**：可以在各种地形移动
4. **持续输出**：触手轮流射击，永不停止

### 弱点：
1. **生命值低**：容易被击杀
2. **护甲低**：受重伤影响大
3. **单一体攻击**：容易被针对性防御
4. **近身弱**：主要依赖远程触手攻击

### 应对策略：
1. **高DPS集中火力**：快速击杀
2. **近身突袭**：阻止触手攻击
3. **破甲武器**：减少护甲影响
4. **持续压制**：不给他调整的机会

## 十、实体文件结构

### YggdrasilLeg类（src/flame/entities/YggdrasilLeg.java）
- 管理单条腿的运动
- IK计算
- 绘制腿部

### YggdrasilTentacle类（src/flame/entities/YggdrasilTentacle.java）
- 管理单条触手
- 瞄准系统
- 射击控制

这些实体类共同构成了Yggdrasil的复杂肢体系统！
