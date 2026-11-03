修复 GTNH 配方页两处版面问题：**长来源名越出面板边框**、**物品格子压住说明文字**。
Mixin 注入 `RecipeDisplayInfo#drawText`，不改动任何上游模组文件，删除 jar 即还原。

### 修复

- **来源行越界** → 按面板实际宽度折行，长 mod 名不再被截断
- **物品压字** → 用该配方表自身的坐标函数算出网格底边，把「时间 / 来源」整体下移到网格下方

仅作用于 GregTech 配方表；普通配方外观不变，未触发时只多一次比较。

### 安装

把下方附件放进 `.minecraft/mods/`。无配置文件、不写盘。

### 兼容

GTNH 2.9.0 正式版 · Java 25 · `+unimixins-all` 1.7.10-0.3.1。其它版本未测试。
字节码目标 Java 8，不依赖 Java 25 特性。

### 校验

```
SHA-256  __SHA256__
```

与附件实际字节一致，由发布流水线现场计算并注入；`dist/` 内对应文件可用同样命令复核。

原理、触发条件、已知限制见 [README](https://github.com/cryspera/NEIRecipeTextWrap#readme)。

LGPL-3.0 · 本模组使用 AI 辅助制作。
