# 创维 Android 6 适配交接

更新：2026-10-22。分支 `skyworth-android6`，基线标签 `skyworth-v5.23`。
这是可随源码保存的交接，不含个人设备地址、签名密钥或厂商二进制。

## 当前基线

- 测试设备：创维/酷开9S52_H9D，Android6/API23，MStar maxim平台；不保证其他机型适用。
- 应用包：`com.gathin.tvhome`；versionCode37，versionName `2.24-skyworth6.4.6`。
- 本地已安装交付包称v5.23，SHA256：
  `CCDC93110C660028F9BFAB240B07D99CEDBB01DBB2C7112563F1DBA3CCD09528`。
  原APK和原签名仅作本地备份，未提交；重新构建不保证字节相同或能覆盖现有签名。
- 用户设备最后状态：黑底、五列常用应用、右上角纯图标；动画和高级效果开启。
- 齿轮单击进入桌面设置，系统设置在里面；无隐藏长按入口。
- 首页时钟屏保5分钟，每日定时关机当前关闭；这些是设备偏好，不代表所有新安装默认值。

## 已完成

1. API26→23适配：网络监听、字体、TV接口兼容，core library desugaring支持旧版Java API。
2. 创维开机：首页启动队列使用原厂Provider接口切换；仅早期开机TV意图转新桌面，普通信号转发原厂SkyTV。没有修改系统APK、boot或系统分区。
3. Home接管：无障碍服务监听指定厂商窗口，保留输入源及原厂睡眠倒计时，不轮询、不持续杀进程。酷开包仍保留并用于原生功能。
4. 视觉：统一16:9品牌应用卡片、玻璃焦点平移和轻回弹、缓存环境光，减少重复缩放与动态阴影开销。并非完整复制全部上游效果。
5. 背景：黑底、六种静态极光、必应每日及固定当前图片；照片顶端遮罩保持时间可读。极光缓存720×405，与参考主题的静态壁纸性质一致。
6. 时钟屏保：只在首页闲置时进入`ClockActivity`，与`ClockDream`复用动态场景；每分钟变换位置，有光晕过渡，退出清理动画。离开首页取消计时，不更改全局休眠/屏保设置。
7. 开机音频补偿：每个内核boot_id只尝试一次静音反转150ms再恢复原值，保留原本静音和音量。不是已证明的音频根因修复。
8. 定时关机：RTC非唤醒闹钟，原厂一分钟倒计时；过期15秒、唯一token、wall/elapsed、重复和亮屏状态保护。

## 验证与限制

- v5.23初版现有118项单元测试、lint、assemble通过；后续场景提取和焦点兼容修正通过lint/assemble。
- 屏保实际2分钟触发、超过一分钟位置与颜色改变、按键退出不误启动应用均已验证。
- Kodi主界面前台等待约165秒，超过测试屏保阈值，仍留在Kodi；未实际播放媒体或听测所有音频场景。
- 最后安装后恢复5分钟，诊断为armed；最终焦点兼容修正后未再完整等5分钟。
- 用户测整机冷启动约25秒。历史重启采样26.81秒起已为新桌面，不等于证明上电全程零闪现或“秒开”。
- 历史信号界面Home采样约0.53秒；v5.23在Kodi中ADB KEYCODE_HOME未立即回首页，需补物理遥控回归，不能直接判定所有路径通过。
- 音频静音状态写入/恢复已读回，真正开机后声音仍需机主听测。
- 黑底24次左右键采样316帧、1卡顿帧、P95 8ms；不同背景不能作为严格跨版本A/B，不能宣称零卡顿。
- 屏保仍是亮屏，不是低功耗待机；插座曾读到约145W，降低背光/节能模式的实际效果尚未测试。
- 秒启动/STR底层研究已按用户要求停止；没有root、刷机或实际挂起验证。不应自动继续。

## 源码导航

Java目录：`launcher/app/src/main/java/com/gathin/tvhome/`。

| 功能 | 文件 |
|---|---|
| 首页、设置 | MainActivity、LauncherSettingsActivity、AppLibraryDialog |
| 创维适配 | CoocaaCompat、CoocaaBootReceiver、CoocaaBootModeReceiver、BootTvInterceptActivity、CoocaaHomeService |
| 背景 | BackgroundSettingsActivity、BingWallpaper、BingWallpaperView、AuroraView、WallpaperPlayer |
| 卡片与动效 | AppAdapter、BrandTileArtwork、TileArtwork、IconTileArtwork、AmbientGlowView、DockDecoration |
| 屏保 | HomeIdleClock、ClockActivity、ClockScene、ClockDream、ScreensaverSettingsActivity |
| 开机声音 | BootAudioRecovery |
| 关机 | ShutdownScheduler、ShutdownAlarmReceiver、ShutdownTiming、ShutdownSettingsActivity |

`ShutdownTimingTest`在`launcher/app/src/test/`，其余相关布局、中文文案和服务配置在`res/`。

## 构建与维护

工程使用JDK17、Gradle8.9、Android SDK34及Build Tools34.0.0。配置本机SDK路径、缓存和签名后，在仓库根目录执行：

```text
cd launcher
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Windows用`gradlew.bat`。已有依赖缓存时可加`--offline`；首次构建通常需要下载依赖。
产物位于`launcher/app/build/outputs/apk/release/app-release.apk`。

- 当前release使用本地debug签名，适合既有私用部署。覆盖安装必须保留原签名；不要用新生成密钥覆盖或卸载清数据。正式发布需另行设计签名及迁移。
- 修改后递增版本号、完整记录构建检查和SHA256；保留当前标签，避免覆盖唯一可用基线。
- 用户另有工作区级工具和完整私人交接，它们不在本仓库；不要假定个人SDK缓存、签名、旧APK随Git自动备份。
- 不盲跑debloat示例，不删除酷开系统包。历史精简列表仅是单机记录。
- 本固件force-stop可能清除无障碍绑定；安装后不仅查看enabled字符串，还需确认`dumpsys accessibility`实际绑定，恢复时保留其他服务。
- 截图用设备`/data/local/tmp/`，不要假定`/sdcard`可用。UI坐标动作前先看截图，性能测试确认焦点在应用卡片。
- `CoocaaBootModeReceiver`的`VENDOR_DIAGNOSTICS`可由具备DUMP权限的ADB读取屏保/声音/关机状态。`RESTORE_STOCK_BOOT`只恢复开机队列，不是完整退出Home接管。

## 下一步建议

1. 用户在家时补冷启动、物理Home、信号切换、开机声音实测；当前无人值守时不反复重启/关机。
2. 屏保补最终版完整5分钟、关闭、交互重置、真实媒体播放与回调释放检查。
3. 关机补取消、跨天、错过、改时钟、重启场景，真实断电测试需用户方便；保持当前关闭偏好。
4. 壁纸补断网缓存、日期时区、重试、固定图保留；排版补长应用名、多列、不同语言资源完整性。
5. 功耗优先调查原厂背光/节能选项，先记录原值，再用插座对比；尚未实施自动调光或闲置关机。
6. 输入源菜单、上游新功能逐项评估API23依赖，不直接全量合并；自研播放器大文件/多声道问题不属于已完成项。

保留上游MIT及字体OFL许可证。厂商类只从已安装服务运行时加载，仓库不包含厂商SDK、反编译文件或固件；发布前另核对素材和依赖许可。
