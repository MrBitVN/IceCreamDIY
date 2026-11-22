# 3D Ice Cream DIY Simulator

Game mô phỏng tự làm kem 3D phong cách low-poly pastel, hoàn toàn Native trên cả hai nền tảng Android và iOS.

---

## 1. Yêu cầu hệ thống

- **Android:**
  - Android Studio Hedgehog / Iguana / Ladybug hoặc mới hơn.
  - JDK 17 (hoặc JDK 21).
  - Android SDK Platform 34 hoặc 35 (minSdk 26).
- **iOS:**
  - macOS với Xcode 15 trở lên.
  - iOS Deployment Target 16.0+.

---

## 2. Cách Build & Run

### Android (`ice_cream_android`)

#### Cách 1: Sử dụng Terminal / Command Line
1. Mở terminal tại thư mục `ice_cream_android`:
   ```bash
   cd ice_cream_android
   ```
2. Build ứng dụng dạng Debug:
   - Trên Windows:
     ```cmd
     .\gradlew.bat assembleDebug
     ```
   - Trên macOS / Linux:
     ```bash
     ./gradlew assembleDebug
     ```
3. Cài đặt và chạy trên thiết bị / emulator đã kết nối:
   ```bash
   .\gradlew.bat installDebug
   ```
   File APK đầu ra sẽ nằm tại: `app/build/outputs/apk/debug/app-debug.apk`.

#### Cách 2: Sử dụng Android Studio
1. Mở Android Studio -> Chọn **Open**.
2. Trỏ tới thư mục `c:/work/IceCreamInc/ice_cream_android`.
3. Chờ Gradle sync hoàn tất, chọn thiết bị/emulator và nhấn nút **Run** (Shift + F10).

---

### iOS (`ice_cream_ios`)

#### Cách 1: Sử dụng Xcode
1. Mở Xcode.
2. Chọn **Open a Project or File** và trỏ đến:
   `c:/work/IceCreamInc/ice_cream_ios/ice_cream_ios.xcodeproj`
3. Chọn máy ảo Simulator (ví dụ: iPhone 15 / iPhone 16) hoặc thiết bị thật.
4. Nhấn **Cmd + R** (Product -> Run) để build và chạy ứng dụng.

#### Cách 2: Sử dụng Terminal (`xcodebuild`)
```bash
cd ice_cream_ios
xcodebuild -project ice_cream_ios.xcodeproj -scheme ice_cream_ios -destination 'platform=iOS Simulator,name=iPhone 16' build
```

---

## 3. Kiến trúc dự án

### Phase 1: Nền tảng, Cảnh 3D & HUD
- **Màn hình:**
  - `SplashScreen`: Logo kem 3D nảy hiệu ứng và tự động chuyển sang Home.
  - `HomeScreen`: Hiển thị Coin hiện tại, Level hiện tại, nút to "PLAY".
  - `GameScreen`: HUD đạt ~95% ảnh tham chiếu (Icon Cài đặt, Icon Restart, Coin Pill vàng, 3 sao, nhãn LEVEL, ô MATCH preview bên trái, và 3 nút vị kem tròn ở đáy).
- **Cảnh 3D Procedural (100% Code):**
  - Giá đỡ chữ T bằng gỗ với vòi rót kem hướng xuống và 3 ống đựng vị kem (Vanilla, Strawberry, Chocolate).
  - Ốc quế (Waffle Cone) sinh mesh nón cụt procedural với texture mạng lưới bánh quế diamond bằng shader.
  - Ốc quế tự động xoay nhẹ (idle rotation) và có đĩa bóng mềm (soft shadow) chiếu xuống sàn.

### Phase 2: Cơ chế Dispense Kem Xoắn & Bện Màu Đa Vị
- **Thuật toán Mesh Extrusion:**
  - `MeshExtruder`: Sinh ống tròn chạy theo đường xoắn ốc (conical helix) quanh trục Y, bán kính xoắn và bán kính ống thon dần theo độ cao.
  - Tiết diện ống có răng khía hình sao (star nozzle fluting 6-8 thùy).
  - **Zero-allocation Streaming:** Bộ đệm Native Vertex/Index Buffer được cấp phát trước cố định (preallocated), mỗi tick chỉ ghi thêm vòng đỉnh mới (ring vertices) vào buffer, không recreate lại toàn bộ mesh mỗi frame.
- **Bện màu đa vị (Multi-flavor Swirling):**
  - Khi giữ đồng thời 2 hoặc 3 nút vị (ví dụ: Strawberry + Chocolate hoặc Vanilla + Strawberry + Chocolate), tiết diện ống được chia thành các cung màu tương ứng với các vị được chọn, tạo thành các dải màu xoắn bện vào nhau y như ảnh tham chiếu 4 & 5.
- **Vòi phun hình sao & Ốc quế tự hạ xuống:**
  - Vòi rót kem (dispenser nozzle) có hình sao 8 khía.
  - Dòng kem động (pouring stream) nối từ vòi rót xuống đỉnh ngọn kem khi đang bấm giữ nút.
  - Ốc quế tự động hạ thấp dần theo độ cao của kem để đỉnh kem luôn nằm ngay dưới đầu vòi rót.
- **Giới hạn độ cao (Max Height):**
  - Khi đạt giới hạn (`pourProgress >= 1.0`), tự động ngắt dispense và dừng dòng kem.
- **Âm thanh & Haptic:**
  - `SoundHapticManager`: Giả lập âm thanh vo ve nhẹ của máy làm kem và rung haptic tactile pulse nhẹ theo nhịp khi đang rót kem.

### Phase 3: Dữ liệu 20 Level, MATCH Preview & Thuật toán Chấm Điểm
- **Định nghĩa Level dạng Data (JSON):**
  - File [levels.json](file:///c:/work/IceCreamInc/ice_cream_android/app/src/main/assets/levels.json) chứa cấu hình 20 level lũy tiến độ khó: danh sách vị khả dụng, kem mẫu (các tầng vị + tỷ lệ độ cao), minigame, số lượng cốm, cherry, thông tin khách hàng.
  - Level 1: 3 vị cơ bản, 1 lớp kem Vanilla duy nhất, kèm bàn tay chỉ dẫn (Tutorial Hand) nảy nhấp nháy hướng dẫn người chơi bấm vào nút Vanilla.
  - Từ Level 15: Xuất hiện vị **Pistachio** (màu xanh cốm pastel) cùng máy rót 3 vị đặc biệt `[Strawberry, Chocolate, Pistachio]`.
- **Ô MATCH Preview:**
  - Render kem mẫu thu nhỏ đồng bộ tỷ lệ với cấu trúc tầng của level: hiển thị các tầng vị đơn hoặc dải xoắn bện đa màu + cherry/sprinkles.
- **Thuật toán Chấm Điểm (`IceCreamMatcher`):**
  - Đánh giá theo công thức chuẩn:
    $$\text{TotalScore} = 0.45 \cdot S_{seq} + 0.35 \cdot S_{prop} + 0.20 \cdot S_{height}$$
  - $S_{seq}$ (45%): So khớp thứ tự các tầng vị bằng chỉ số Jaccard Similarity giữa các vị đã rót và yêu cầu; phạt trừ điểm nếu rót thiếu/thừa tầng.
  - $S_{prop}$ (35%): So khớp độ dày/tỷ lệ độ cao từng tầng so với mẫu.
  - $S_{height}$ (20%): Kiểm tra độ đầy đặn của cây kem so với ốc quế.
  - Quy đổi: $\ge 85 \to 3$ sao, $60 - 84 \to 2$ sao, $40 - 59 \to 1$ sao, $< 40 \to 0$ sao (làm lại).

### Phase 4: Mini-Game Chuẩn Bị Nguyên Liệu & Trang Trí Topping
- **Mini-game Phân Loại Nguyên Liệu (Ảnh 4):**
  - Thớt gỗ tự nhiên bo góc chứa các nguyên liệu 3D procedural (Thanh sô-cô-la có rãnh kèm mặt cười emoji, quả dâu tây đỏ chấm hạt, hoa phong lan vani).
  - Hộp chia 3 ngăn: kéo thả nguyên liệu vào đúng ngăn, hỗ trợ hit-testing với hiệu ứng snap đàn hồi nảy nảy (elastic bounce).
  - Thanh tiến độ tích hợp các mốc thưởng +5 và +10 coin văng coin vào ví, hoàn thành tự động chuyển cảnh sang rót kem.
- **Trang Trí (Ảnh 8):**
  - Xếp chồng ốc quế (Cone Stacking) lồng nhau giật cấp khi `extraCones >= 2`.
  - Hạt cốm kẹo hình trụ pastel rải bám theo các rãnh xoắn của kem.
  - Quả Cherry đỏ tươi low-poly kèm cuống cong xanh cắm trên đỉnh kem.

### Phase 5: Khách Hàng Low-Poly, Particle Tiền Rơi, Kết Quả & Cài Đặt
- **Khách hàng Low-poly 3D Procedural (Ảnh 6 & 7):**
  - Dựng từ các primitive: đầu, tóc cách điệu, kính râm thời trang, thân áo, cổ, hai cánh tay.
  - 5 biến thể ngoại hình đặc sắc:
    1. *Cool Girl* (Ảnh 7): Tóc tím/xanh dài, kính râm đen, áo sát nách màu mint ngọc lam.
    2. *Hipster Guy* (Ảnh 6): Tóc nâu cắt ngắn, áo khoác đỏ burgundy bên ngoài áo phông xanh bạc hà.
    3. *Sunny Blonde*: Tóc vàng buộc đuôi ngựa, áo thun màu hồng san hô.
    4. *Trendy Guy*: Tóc nâu vuốt ngược pompadour, kính râm, áo polo vàng mù tạt.
    5. *Sporty Teen*: Mũ lưỡi trai xanh ngọc, áo hoodie cam pastel.
- **Animation & Phản Ứng:**
  - Khách hàng đứng sau quầy bar hồng, tay trái cầm ly/ốc quế kem đã làm.
  - Phản ứng theo số sao: Khi đạt 3 sao, khách hàng giơ ngón cái (Thumbs up pose) ngầu và gật đầu hài lòng!
  - **Particle Tiền Rơi:** 16 tờ tiền đô-la xanh pastel ($) rơi từ trên cao xuống, chao đảo xoay lật trong không gian 3D và đáp xuống mặt bàn quầy bar (như Ảnh 7).
- **Màn Hình Kết Quả (Results Screen):**
  - Hiệu ứng bung nảy 3 ngôi sao vàng kim, nhãn đánh giá ("PERFECT!", "GREAT JOB!").
  - Huy hiệu cộng thưởng coin (+100, +60, +30 coin) với số đếm tăng dần.
  - Nút bấm `NEXT LEVEL` màu xanh lá rực rỡ và nút `Replay`.
- **Cài Đặt & Lưu Trữ:**
  - Modal Settings (nút bánh răng góc trái trên): Bật/Tắt âm thanh hiệu ứng, Bật/Tắt rung phản hồi haptic, Reset tiến trình chơi.
  - Lưu tiến trình tự động bằng Jetpack DataStore (Android) và UserDefaults (iOS).
- **Nền Cửa Hàng Kem Hồng - Mint (Ảnh 5-7):**
  - Từ Level 5 trở đi, kích hoạt không gian nội thất cửa hàng kem hoàn chỉnh: tường hồng kẻ lam gỗ ngang, tranh treo tường kem ốc quế, máy làm kem màu ngọc lam bạc hà với cần gạt bạc và quầy bar hồng pastel ở phía trước.
- **Tối Ưu Hiệu Năng & Ổn Định:**
  - Khung hình duy trì mượt mà 60 FPS, không cấp phát bộ nhớ động trong render loop.
  - Xử lý hoàn hảo lifecycle khi xoay màn hình hoặc đưa app vào background.
