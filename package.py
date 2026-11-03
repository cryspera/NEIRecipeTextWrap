"""把 out/ 下的编译产物打成一个能被 FML / RFB 识别的 Mixin 容器 jar。

只负责打包，不负责编译；编译见 build.sh。产物落在 build/ 下。
"""

import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
# 只收口 src/ 的编译结果；stubs/ 是编译期替身，绝不能进 jar
CLASSES = os.path.join(ROOT, 'build', 'classes')
DIST = os.path.join(ROOT, 'build')

MOD_ID = 'neiwrap'
VERSION = '1.1.1'
# 必须与 src/ 下的真实目录结构一致：Mixin 会在加载期按该包名反射查找混入类
MIXIN_PACKAGE = 'com.cryspera.neiwrap.mixin'

OUT = os.path.join(DIST, 'NEI-Recipe-Text-Wrap-%s.jar' % VERSION)

# 关键：FML/RFB 只会为「带 TweakClass / FMLCorePlugin 的 jar」创建 Mixin 容器，
# 容器创建后才会去读 MixinConfigs。缺少 TweakClass 时配置完全不生效（静默失败）。
manifest_lines = [
    'Manifest-Version: 1.0',
    'TweakClass: org.spongepowered.asm.launch.MixinTweaker',
    'MixinConfigs: mixins.%s.json' % MOD_ID,
    'ForceLoadAsMod: true',
]
for line in manifest_lines:
    assert len(line.encode('utf-8')) <= 72, 'MANIFEST 行超过 72 字节: ' + line
manifest = ''.join(l + '\r\n' for l in manifest_lines) + '\r\n'

mixin_cfg = '''{
  "required": false,
  "minVersion": "0.8",
  "package": "%s",
  "compatibilityLevel": "JAVA_8",
  "mixins": [
    "MixinRecipeDisplayInfo"
  ],
  "injectors": {
    "defaultRequire": 0
  }
}
''' % MIXIN_PACKAGE

os.makedirs(DIST, exist_ok=True)
if os.path.exists(OUT):
    os.remove(OUT)

added = 0
with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('META-INF/MANIFEST.MF', manifest)
    z.writestr('mixins.%s.json' % MOD_ID, mixin_cfg)
    for root, dirs, files in os.walk(CLASSES):
        for f in files:
            if not f.endswith('.class'):
                continue
            p = os.path.join(root, f)
            z.write(p, os.path.relpath(p, CLASSES).replace(os.sep, '/'))
            added += 1

print('built', OUT, os.path.getsize(OUT), 'bytes,', added, 'classes')
print(zipfile.ZipFile(OUT).namelist())
