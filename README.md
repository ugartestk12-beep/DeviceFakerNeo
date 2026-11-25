# DeviceFaker Neo (LSPosed Module)

Module LSPosed hiện đại (Android 8–15) thay thế hoàn toàn cho Android Faker thế hệ cũ:
- **Chọn từng app hoặc nhiều app cùng lúc** (chạy độc lập trên điện thoại, không cần PC, không cần khởi chạy app qua tool).
- **Hỗ trợ cả Cấu hình Chung (Global) lẫn Cấu hình Riêng cho từng App (Per-App Override)**.
- **Không đụng `system.prop` / `.Integrity Box` / `Tricky Store`** → **0% Bootloop**, an toàn tuyệt đối.
- **Đầy đủ các lớp Hook sâu**: `Build`, `SystemProperties`, `Settings.Secure` (Android ID), `TelephonyManager` (IMEI Luhn, IMSI, ICCID, Phone, SIM Operator/Country/Name), `WifiInfo` & `NetworkInterface` (MAC/SSID/BSSID), `BluetoothAdapter` (BT MAC), `GSF ID`, `GAID`.

---

## Cách Build ra file `.apk` tự động bằng GitHub Actions (Không cần cài Android Studio)

1. Tạo một repository mới trên [GitHub](https://github.com/new) (chọn Private hoặc Public đều được), ví dụ tên `DeviceFakerNeo`.
2. Mở terminal trong thư mục `DeviceFakerNeo-LSPosed` này và chạy:
   ```bash
   git init
   git add .
   git commit -m "Initial DeviceFaker Neo LSPosed Module"
   git branch -M main
   git remote add origin https://github.com/<TEN_GITHUB_CUA_BAN>/DeviceFakerNeo.git
   git push -u origin main
   ```
3. Truy cập tab **Actions** trên GitHub → chọn workflow **Build DeviceFaker Neo APK** (chạy khoảng 1–2 phút).
4. Sau khi build xong (dấu tích xanh ✅), kéo xuống mục **Artifacts** → tải **`DeviceFakerNeo-LSPosed-APK`** về, giải nén lấy `app-debug.apk`.
5. Cài lên điện thoại:
   ```bash
   adb install app-debug.apk
   ```
6. Mở **LSPosed Manager** → **Modules** → bật **DeviceFaker Neo** → tick chọn các ứng dụng muốn fake → mở app **DeviceFaker Neo** chỉnh thông số / bấm **Random** → **Lưu cấu hình** → tắt và mở lại ứng dụng mục tiêu là xong!
