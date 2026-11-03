#!/usr/bin/env bash
# 编译 + 打包 NEI Recipe Text Wrap。
#
# 编译期需要三个第三方 jar（不入库，需自备）：
#   gregtech-*.jar        gregtech.api.recipe.* / gregtech.nei.RecipeDisplayInfo
#   modularui-*.jar       com.gtnewhorizons.modularui.api.math.Pos2d
#   +unimixins-*.jar      org.spongepowered.asm.*
# 运行期还需要 NotEnoughItems，编译期只在类路径里占位（本工程不直接引用其符号）。
#
# 用法：
#   ./build.sh
#   MC_MODS=/path/to/.minecraft/mods ./build.sh
set -euo pipefail
cd "$(dirname "$0")"

MC_MODS="${MC_MODS:-D:/BaiduNetdiskDownload/PrismLauncher/instances/GT_New_Horizons_2.9.0_Java_17-26/.minecraft/mods}"

pick() {
    local hit
    hit=$(ls "$MC_MODS"/$1 2>/dev/null | head -1)
    if [ -z "$hit" ]; then
        echo "缺少依赖：$1 （在 $MC_MODS 下未找到）" >&2
        exit 1
    fi
    echo "$hit"
}

GT=$(pick 'gregtech-*.jar')
MUI=$(pick 'modularui-*.jar')
MIXIN=$(pick '+unimixins-all-*.jar')
NEI=$(pick 'NotEnoughItems-*.jar')

CP="$GT;$MUI;$MIXIN;$NEI"

# stubs/ 只是编译期替身（用 SRG 名声明 MC 成员），单独编译到一个目录，
# 只作为 classpath 参与 src 的编译，绝不进最终 jar。
rm -rf build/classes build/stubs
mkdir -p build/classes build/stubs
javac --release 8 -encoding UTF-8 -nowarn -d build/stubs $(find stubs -name '*.java')
javac --release 8 -encoding UTF-8 -nowarn -cp "build/stubs;$CP" -d build/classes $(find src -name '*.java')

PY=$(command -v python3 || command -v python || command -v py)
"$PY" package.py
