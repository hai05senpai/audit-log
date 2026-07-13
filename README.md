# Audit Log System

Hệ thống quản lý và giám sát Audit Log (Nhật ký kiểm toán) dành cho các hệ thống doanh nghiệp. Dự án bao gồm Backend (Spring Boot), Frontend (React/Next.js) và các dịch vụ bổ trợ chạy trên Docker.

## 🚀 Hướng dẫn khởi chạy Project

### 1. Khởi chạy các dịch vụ bổ trợ (Docker)

Di chuyển vào thư mục dự án và khởi chạy các container cần thiết (Cơ sở dữ liệu, Message Broker, v.v.):

```bash
cd auditlog
docker compose up -d
```

* **Kiểm tra trạng thái:** Chạy lệnh `docker ps` để đảm bảo các container đều đang ở trạng thái *Up*.
* **Xử lý sự cố:** Nếu xảy ra lỗi hoặc muốn làm sạch container, bạn có thể hạ hệ thống xuống rồi khởi chạy lại:
  ```bash
  docker compose down
  ```

### 2. Khởi chạy Backend (Spring Boot)

Mở một terminal mới, di chuyển vào thư mục backend và khởi chạy ứng dụng:

```bash
cd auditlog
./mvnw clean spring-boot:run
```

*Lưu ý: Đảm bảo cổng chạy Backend (thường là `8080`) không bị trùng với ứng dụng khác.*

### 3. Khởi chạy Frontend

Mở một terminal mới để cấu hình và khởi chạy giao diện người dùng.

#### ĐIỀU KIỆN TIÊN QUYẾT (Prerequisites)

Trước khi chạy Frontend, máy tính của bạn cần cài đặt sẵn:

* **Node.js** (Khuyến nghị phiên bản LTS mới nhất)
* **npm** (Đi kèm khi cài Node.js) hoặc **yarn / pnpm**

#### CÁC BƯỚC KHỞI CHẠY:

1. **Di chuyển vào thư mục Frontend:**

   ```bash
   cd auditlog/audit-log-ui
   ```
2. **Cài đặt các thư viện phụ thuộc (Dependencies):**
   *Nếu đây là lần đầu tiên bạn clone project hoặc khi project có cập nhật thư viện mới, bắt buộc phải chạy lệnh này để cài đặt thư mục `node_modules`:*

   ```bash
   npm install
   ```
3. **Khởi chạy môi trường Phát triển (Development Mode):**

   ```bash
   npm run dev
   ```
4. **Truy cập Giao diện:**
   Sau khi terminal báo chạy thành công, hãy mở trình duyệt và truy cập vào đường dẫn:

   * Mặc định thường là: **`http://localhost:5173`** (đối với Vite) hoặc **`http://localhost:3000`** (đối với Next.js/CRA).
   * *(Xem chính xác địa chỉ URL được hiển thị trên màn hình terminal của bạn)*

---

## 🛠️ Công nghệ sử dụng

* **Backend:** Java, Spring Boot, Spring Data JPA, JWT Authentication
* **Frontend:** ReactJS, Tailwind CSS, TypeScript
* **DevOps/Database:** Docker, Docker Compose, Elasticsearch
