# Flame Out

Mindustry v154/v158 兼容模组。

## 单位

- **Empathy（共鸣）** - 可直接生成的强力单位，支持多种攻击模式
- **Apathy（冷漠）** - 需要击杀后召唤共鸣
- **Despondency（消沉）** - 最终Boss
- **Yggdrasil（世界树）** - 大型支援单位

## 剧情

按 `V` 键启动剧情，配合以下快捷键：
- `Z` - 重置剧情
- `X` - 前进到下一阶段
- `C` - 退出剧情
- `B`（按住）- 快进

## 构建

```powershell
# 桌面版
.\gradlew jar

# Android版（需要Android SDK）
.\gradlew deploy
```

输出文件在 `build/libs/flameout.jar`

## 版本

- 桌面版：v154 / v158
- 手机版：需要通过GitHub Actions构建
