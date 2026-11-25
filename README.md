# 3D Ice Cream DIY (Native Android & iOS)

Game mô phỏng làm kem 3D phong cách **low-poly pastel**, xây dựng **100% Native** trên cả hai nền tảng, không sử dụng Flutter, React Native, Unity hay game engine bên thứ ba.

- **Android (`ice_cream_android/`):** Kotlin, Jetpack Compose (UI), OpenGL ES 3.0 (`GLSurfaceView`), Jetpack DataStore, `minSdk 26`, `targetSdk 35`.
- **iOS (`ice_cream_ios/`):** Swift, SwiftUI (UI), SceneKit (`SCNView`), UserDefaults, iOS 16+.
- **Shared Level Data (`shared/levels.json`):** Nguồn dữ liệu chung cho 20 level (cả 2 app parse cùng schema JSON).

---

## Cấu trúc thư mục

```text
IceCreamInc/
├── shared/
│   └── levels.json                  # Cấu hình 20 level dùng chung cho Android & iOS
├── ice_cream_android/               # Project Android Native (Kotlin + Compose + GLES 3.0)
│   └── app/src/main/java/com/icecream/diy/
│       ├── data/                    # Models, LevelDataProvider, DataStoreProgressRepository
│       ├── domain/                  # MeshExtruder, MinigameEngine, IceCreamMatcher, SoundHapticManager
│       ├── presentation/            # Compose Screens, Components, Theme, GameViewModel
│       └── scene3d/                 # GLSurfaceView renderers & Procedural Mesh Generators
├── ice_cream_ios/                   # Project iOS Native (Swift + SwiftUI + SceneKit)
│   ├── ice_cream_ios.xcodeproj/
│   └── ice_cream_ios/
│       ├── Data/                    # Models, LevelDataProvider, UserDefaultsProgressRepository
│       ├── Domain/                  # MeshExtruder, MinigameEngine, IceCreamMatcher, SoundHapticManager
│       ├── Presentation/            # SwiftUI Screens, Components, Theme, GameViewModel
│       └── Scene3D/                 # SCNView wrappers & Procedural SCNGeometry Generators
└── README.md
```

---

## Hướng dẫn Build & Run

### 1. Android
Yêu cầu: **Android Studio Ladybug+** hoặc **JDK 17+** và Android SDK (`compileSdk 35`, `minSdk 26`).

```bash
cd ice_cream_android

# Chạy Unit Test (IceCreamMatcherTest, v.v.)
./gradlew test

# Kiểm tra Lint và Build APK Release
./gradlew lint assembleRelease

# Build & Cài APK Debug lên thiết bị/emulator
./gradlew installDebug
```

> **Ghi chú:** File `shared/levels.json` được cấu hình qua `sourceSets.main.assets.srcDirs("../../shared")` trong `app/build.gradle.kts`, không cần copy thủ công.

### 2. iOS
Yêu cầu: **macOS** với **Xcode 15+** và **iOS 16.0+ Simulator / Device**.

1. Mở `ice_cream_ios/ice_cream_ios.xcodeproj` bằng Xcode.
2. Chọn target `ice_cream_ios` và Simulator (ví dụ iPhone 15, iOS 17+) hoặc thiết bị thật.
3. Nhấn `Cmd + R` để build và chạy.

Hoặc build/archive qua CLI trên macOS:

```bash
cd ice_cream_ios

# Build cho Simulator
xcodebuild -project ice_cream_ios.xcodeproj \
  -scheme ice_cream_ios \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  build

# Archive Release
xcodebuild -project ice_cream_ios.xcodeproj \
  -scheme ice_cream_ios \
  -configuration Release \
  -archivePath build/ice_cream_ios.xcarchive \
  archive
```

---

## Tối ưu Hiệu năng & Lifecycle (60 FPS)

- **Zero Allocation in Render Loop:** `MeshExtruder` cấp phát sẵn `FloatArray` / `ShortArray` và `ByteBuffer.allocateDirect` từ đầu; mỗi frame khi rót chỉ ghi thêm một vòng đỉnh mới (`glBufferSubData` / preallocated native buffer), không khởi tạo lại mesh hay `FloatArray` trong `onDrawFrame`.
- **Lifecycle & Memory:**
  - `GLSurfaceView` bật `preserveEGLContextOnPause = true` và gắn `LifecycleEventObserver` (`ON_RESUME` / `ON_PAUSE`).
  - `MainActivity` sử dụng `ViewModelProvider` để giữ trạng thái khi xoay màn hình / đổi cấu hình, đồng thời dừng timer rót kem & âm thanh trong `onPause()` / `onTrimMemory()`.
  - iOS `ContentView` lắng nghe `@Environment(\.scenePhase)` để gọi `viewModel.onAppBackgrounded()` khi app vào `.background` hoặc `.inactive`.

---

## Các Placeholder còn lại

1. **Âm thanh (Audio SFX):**
   - Hiện tại `SoundHapticManager` dùng `AudioTrack` tổng hợp sóng sine nhẹ (Android) và `AudioServicesPlaySystemSound` (iOS) làm feedback cơ bản; chưa tích hợp file `.wav`/`.ogg` thu âm chuyên nghiệp hoặc nhạc nền BGM.
2. **Quảng cáo (Ads / Rewarded Video):**
   - Nút **2X COINS** trên màn Kết quả là placeholder: bấm vào sẽ nhân đôi coin thưởng ngay lập tức mà không gọi SDK quảng cáo (AdMob / AppLovin).
3. **In-App Purchase & Meta Features (Home Screen):**
   - Các icon **VIP**, **Shop**, **Spin** ở cột phải `HomeScreen` hiện là placeholder bấm được (chưa mở cửa hàng skin ốc quế/máy rót hoặc vòng quay may mắn thật, chưa gắn StoreKit / Google Play Billing).
