# Slide Presentation Outline: Movie Recommendation Intelligence Platform
> **Pitch Deck for MUGVN Mini Hackathon 2026**
> Consistently structured to be copied directly into Google Slides and exported to Microsoft PowerPoint (`.pptx`).

---

## 🎨 MongoDB Design Token & Styling Guide (For Google Slides / PPTX)

To ensure this slide deck matches the premium visual identity of MongoDB, apply the following design systems during creation:

| Component Type | Slide Theme | Background Color | Primary Text | Accent / Highlight | Font Family | Shapes & Corner Radius |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Hero / Title / Wrap-up** | Dark (Deep Teal) | `#001e2b` (Deep Teal) | `#ffffff` (White) | `#00ed64` (MongoDB Green) | **Euclid Circular A** | Buttons/Pills: `rounded.full` (pill shape) |
| **Content / Body** | Light (Stark) | `#ffffff` (White) | `#001e2b` (Deep Teal) | `#00684a` (Green Dark) | **Euclid Circular A** | Cards/Blocks: `rounded.lg` (12px radius) |
| **Highlight / Callout** | Mint Accent | `#e3fcef` (Soft Mint) | `#00684a` (Green Dark) | `#00ed64` (MongoDB Green) | **Euclid Circular A** | Cards/Borders: `rounded.lg` (12px), 2px border |

### 🏷️ Category Accent Colors (Use for Labels / Icons)
* 🧡 **Accent Orange (`#fa6e39`)**: Search, Discovery, Mood & Emotion topics.
* 💜 **Accent Purple (`#7b3ff2`)**: Database, Security, VPC, & Infrastructure topics.
* 💙 **Accent Blue (`#3d4f9f`)**: AWS Cloud, Scalability & DevOps workflows.
* 💗 **Accent Pink (`#f06bb8`)**: Personalization & User Behavior topics.

---

# 🗂️ Slide-by-Slide Contents

---

## 🛝 Slide 1: Title Slide (Dark Theme)

*   **Slide Theme**: Dark Mode (Deep Teal `#001e2b` Background)
*   **Layout**: Center-aligned heavy typography with a bottom floating code mockup card.
*   **Visual Elements**:
    *   Main title in bold, thick letters.
    *   Sub-title in muted gray-white.
    *   A small bright MongoDB Green (`#00ed64`) badge at the top saying `"MUGVN MINI HACKATHON 2026"`.

---

### 📝 Slide Copy (Vietnamese & English)

#### **[HEADER BADGE]**
🟢 `MUGVN MINI HACKATHON 2026` *(Text color: `#001e2b` on `#00ed64` green pill background)*

#### **[MAIN TITLE]**
# One Data Platform. Unlimited Movie Discovery Potential.
## Nền tảng Tìm kiếm ngữ nghĩa & Đề xuất phim Thông minh thế hệ mới

#### **[SUB-TITLE]**
> Traditional platforms know what users clicked. We understand *why* they liked it.
> *Các nền tảng truyền thống chỉ biết khách hàng đã click gì. Chúng tôi hiểu tại sao họ thích phim đó.*

#### **[FOOTER MOCKUP CARD]**
```bash
$ mongosh "mongodb+srv://atlas..." --eval "db.movies.find({ $vectorSearch: ... })"
[✓] Semantic Discovery Engine: Online
[✓] Real-time Behavioral Scoring Pipeline: Active
[✓] Dual-mode AWS Fargate Cluster: Healthy
```

---

## 🛝 Slide 2: The Core Problem – Traditional Movie Search is Broken (Light Theme)

*   **Slide Theme**: Light Mode (Stark White `#ffffff` Background)
*   **Layout**: Two-column layout (Left: "Vấn đề của tìm kiếm truyền thống", Right: "Vấn đề của hệ thống gợi ý tĩnh").
*   **Visual Elements**:
    *   Use **Accent Orange (`#fa6e39`)** for titles/drawings of the problems to indicate friction.
    *   A gray card on each side with `12px` rounded corners and a subtle `1px` border (`#e1e5e8`).

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`PROJECT INTRODUCTION & PROBLEM STATEMENT`

### **[SLIDE TITLE]**
## The Discovery Fatigue: Tại sao người dùng rời bỏ các trang xem phim?

---

### **[COLUMN 1: TRUY VẤN TỪ KHÓA NGHÈO NÀN (LEXICAL SEARCH LIMITATION)]**
*   **Vấn đề**: Hầu hết các trang phim hiện tại (trừ Netflix) chỉ tìm kiếm theo kiểu so khớp chuỗi cứng nhắc: `SELECT * FROM movies WHERE name LIKE '%abc%'`.
*   **Hậu quả**:
    *   Thất bại khi tìm kiếm theo ý nghĩa hoặc cảm xúc (ví dụ: gõ *"phim viễn tưởng u tối hại não giống Interstellar"* sẽ trả về 0 kết quả).
    *   Không xử lý được viết sai chính tả, từ đồng nghĩa hoặc mô tả bằng ngôn ngữ tự nhiên.
    *   Gây ức chế và tốn thời gian cho người dùng (Search Decision Fatigue).

---

### **[COLUMN 2: ĐỀ XUẤT TĨNH, THIẾU CÁ NHÂN HÓA (STATIC RECOMMENDATIONS)]**
*   **Vấn đề**: Gợi ý phim rập khuôn dựa trên lượt xem tổng hợp (Trending chung) hoặc do Biên tập viên lựa chọn thủ công.
*   **Hậu quả**:
    *   Không ghi nhận hành vi thời gian thực (ví dụ: người dùng vừa nhấn Like 2 phim trinh thám thì trang chủ vẫn đề xuất phim hoạt hình hài hước).
    *   Bỏ qua sở thích ngầm (Implicit Preferences) được thể hiện qua các hành động lướt, click, lưu (Save) hay đánh giá (Rate).
    *   Trải nghiệm vô hồn, thiếu sự đồng hành với từng cá nhân.

---

## 🛝 Slide 3: Brainstorming – The Hidden Pitfalls of Discovery Systems (Light Theme)

*   **Slide Theme**: Light Mode (Stark White `#ffffff` Background)
*   **Layout**: 3-column grid of rounded cards (`12px` border-radius) representing the deeper technical and UX problems that our platform explicitly solves.
*   **Visual Elements**:
    *   Icons colored with **Accent Pink (`#f06bb8`)**, **Accent Orange (`#fa6e39`)**, and **Accent Purple (`#7b3ff2`)**.

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`DEEPER BRAINSTORMING & MARKET ANALYSIS`

### **[SLIDE TITLE]**
## 3 Điểm nghẽn kỹ thuật ẩn giấu trong các hệ thống Discovery hiện nay

---

### **[CARD 1: HỘP ĐEN ĐỀ XUẤT (THE BLACK BOX DILEMMA)]**
*   **Vấn đề**: Các hệ thống AI hiện đại đưa ra danh sách đề xuất nhưng không giải thích lý do.
*   **Hậu quả**: Người dùng nghi ngờ tính khách quan, cảm thấy bị áp đặt và khó chịu.
*   *👉 Giải pháp của chúng tôi:* **Explainability Engine** - Gắn nhãn lý do thời gian thực trực quan (ví dụ: *"Bởi vì bạn thích chủ đề du hành thời gian"*).

### **[CARD 2: KHỦNG HOẢNG KHỞI ĐẦU LẠNH (COLD-START SYSTEM CRASH)]**
*   **Vấn đề**: Người dùng mới (chưa có lịch sử) hoặc Phim mới (chưa có tương tác) bị cô lập khỏi hệ thống đề xuất.
*   **Hậu quả**: Trang chủ trống rỗng (Empty States), phim mới sản xuất không tiếp cận được khán giả.
*   *👉 Giải pháp của chúng tôi:* **Deterministic Hybrid Fallback** - Tự động kích hoạt cơ chế đệm đa tầng (Trending + Cụm thể loại đa dạng) ngay lập tức.

### **[CARD 3: PHÂN MẢNH HẠ TẦNG DỮ LIỆU (INFRASTRUCTURE SILOS)]**
*   **Vấn đề**: Doanh nghiệp phải duy trì SQL cho Metadata, Elasticsearch cho Tìm kiếm, Pinecone cho Vector, và Redis cho Cache.
*   **Hậu quả**: Đồng bộ trễ (Data Lag), chi phí vận hành (Cloud cost) khổng lồ, rủi ro đứt gãy dữ liệu cực cao.
*   *👉 Giải pháp của chúng tôi:* **MongoDB Atlas Single Platform** - Hợp nhất toàn bộ dữ liệu, Vector Search và phân tích thời gian thực trên một CSDL duy nhất.

---

## 🛝 Slide 4: The Solution – MongoDB Atlas-First Intelligence Engine (Dark Theme)

*   **Slide Theme**: Dark Mode (Deep Teal `#001e2b` Background)
*   **Layout**: Left-heavy display text with a visual diagram of the MongoDB feature mapping on the right.
*   **Visual Elements**:
    *   MongoDB Green (`#00ed64`) accents to show features and high-value highlights.
    *   Borders on dark cards use `#1c2d38` (Hairline Dark).

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`THE CORE SOLUTION`

### **[SLIDE TITLE]**
## Hợp nhất Tìm kiếm & Đề xuất trên nền tảng MongoDB Atlas

---

### **[LEFT SIDE: GIẢI PHÁP ĐỘT PHÁ]**
*   **Tìm kiếm Ngữ nghĩa (Semantic Search)**: Sử dụng **MongoDB Vector Search** kết hợp với Spring AI để chuyển đổi câu truy vấn tự nhiên thành vector. Tìm kiếm theo ý nghĩa thực sự thay vì so khớp từ khóa.
*   **Cá nhân hóa theo Hành vi (Behavior-Aware Personalization)**: Theo dõi sự kiện người dùng (`like`, `save`, `rate`, `click`) thời gian thực, lưu trữ bất biến và tổng hợp tức thời qua **MongoDB Aggregation Pipeline**.
*   **Hạ tầng Tối giản (Zero-Sync Architecture)**: Loại bỏ hoàn toàn sự phức tạp của việc đồng bộ hóa dữ liệu giữa các dịch vụ CSDL khác nhau.

---

### **[RIGHT SIDE: MAPPING TÍNH NĂNG MONGODB]**
*   🟢 **`$vectorSearch`** ➔ Truy xuất phim tương đồng ngữ nghĩa chỉ trong mili-giây.
*   🟢 **`Aggregation Pipeline`** ➔ Rerank (Tái xếp hạng) đề xuất dựa trên sở thích ngầm của phiên lướt web hiện tại.
*   🟢 **`$merge`** ➔ Vật chất hóa (Materialize) danh sách phim xu hướng mỗi ngày để tối ưu hóa hiệu năng đọc của demo.
*   🟢 **`Unified Document Model`** ➔ Lưu trữ gọn gàng Catalog phim, Event Log, Profile người dùng trên cùng một cụm Cluster.

---

## 🛝 Slide 5: Production-Ready Project Architecture (Light Theme)

*   **Slide Theme**: Light Mode (Stark White `#ffffff` Background)
*   **Layout**: Unified Architecture Diagram outlining requests flowing from Client to Backend to MongoDB Atlas.
*   **Visual Elements**:
    *   A clean technical map.
    *   Use **Accent Purple (`#7b3ff2`)** for Core VPC & backend resources.
    *   Use **Accent Blue (`#3d4f9f`)** for AWS Edge resources.

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`CLOUD INFRASTRUCTURE`

### **[SLIDE TITLE]**
## AWS Cloud & MongoDB Atlas Production Architecture

---

### **[THE ARCHITECTURE COMPONENTS]**

```
                       [ USER / CLIENT ]
                               ↓
  [ ROUTE 53 ] ➔ [ CLOUDFRONT (CDN) ] ➔ [ WAF (Web Application Firewall) ]
                               ↓
                       [ GUARDDUTY (IDS) ]
                               ↓
                [ ALB (Application Load Balancer) ]
                               ↓ (SSL Termination)
             ┌───────────────── VPC ──────────────────┐
             │                                        │
             │   [ ECS FARGATE (Spring Boot API) ]    │
             │           ↓               ↓            │
             │     [ REDIS CACHE ]   [ S3 BUCKET ]    │
             │                                        │
             └────────────────────────────────────────┘
                               ↓ (Secure Link)
                 [ MONGODB ATLAS (Hosted Cloud) ]
```

---

### **[INFRASTRUCTURE SUMMARY]**
*   **Edge Protection & Delivery**: **Route53** quản lý DNS, **CloudFront** tăng tốc phân phối assets tĩnh từ **S3**, bảo vệ bởi tường lửa ứng dụng **WAF** và quét hiểm họa tự động bằng **GuardDuty**.
*   **Compute & Security Group**: Cụm API Spring Boot chạy trên **ECS Fargate** không máy chủ đặt trong **VPC** riêng tư. Load Balancer (**ALB**) điều phối lưu lượng an toàn.
*   **Caching & State**: **Redis** quản lý session lướt web ẩn danh nhanh chóng, **Secrets Manager** quản lý mã hóa toàn bộ API keys & DB URI kết nối, **CloudWatch** theo dõi và giám sát toàn bộ log hệ thống.
*   **Unified Database**: **MongoDB Atlas** đóng vai trò là "trái tim" dữ liệu lưu trữ catalog phim, event logs, và user profiles.

---

## 🛝 Slide 6: Continuous Infrastructure Delivery (IaC Workflow) (Light Theme)

*   **Slide Theme**: Light Mode (Stark White `#ffffff` Background)
*   **Layout**: Horizontal step-by-step progress chain with 5 blocks using **Accent Blue (`#3d4f9f`)**.
*   **Visual Elements**:
    *   Connected arrows showing the flow of Git commit to cloud deployment.
    *   A callout box at the bottom explaining the benefits of IaC.

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`DEVOPS PIPELINE - PART 1`

### **[SLIDE TITLE]**
## Infrastructure as Code (IaC) Workflow

---

### **[THE PIPELINE CHAIN]**

```
┌───────────────┐     ┌───────────────┐     ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│ 1. Developer  │  ➔  │  2. GitHub    │  ➔  │ 3. GH Actions │  ➔  │ 4. Terraform  │  ➔  │    5. AWS     │
│  Writes IaC   │     │ Secure Repo   │     │  Runner Plan  │     │ Apply State   │     │ Infrastructure│
└───────────────┘     └───────────────┘     └───────────────┘     └───────────────┘     └───────────────┘
```

---

### **[WORKFLOW DETAILS]**
1.  **Developer**: Lập trình viên định nghĩa toàn bộ hạ tầng (VPC, ECS Fargate, WAF, ALB) bằng mã nguồn khai báo HCL (Terraform).
2.  **GitHub**: Mã nguồn Terraform được đẩy lên kho lưu trữ bảo mật của GitHub để kiểm soát phiên bản (Version Control).
3.  **GitHub Actions**: Kích hoạt tự động pipeline chạy kiểm tra cú pháp (`terraform validate`) và lập kế hoạch thay đổi hạ tầng (`terraform plan`).
4.  **Terraform**: Tự động áp dụng các thay đổi (`terraform apply`) lên môi trường Cloud, đảm bảo tính nhất quán của trạng thái (State locking).
5.  **AWS Infrastructure**: Hạ tầng được thiết lập chuẩn xác 100% so với thiết kế bản vẽ mà không cần bất kỳ thao tác thủ công nào trên AWS Console.

---

## 🛝 Slide 7: Continuous Application Deployment (App CI/CD) (Light Theme)

*   **Slide Theme**: Light Mode (Stark White `#ffffff` Background)
*   **Layout**: Vertical flow chart demonstrating Containerization and Blue-Green/Rolling deployment on ECS Fargate.
*   **Visual Elements**:
    *   Use cards with `12px` rounded corners and subtle shadow (`Level 2: rgba(0, 30, 43, 0.08) 0px 4px 12px 0px`).
    *   Highlight key steps with **Accent Purple (`#7b3ff2`)**.

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`DEVOPS PIPELINE - PART 2`

### **[SLIDE TITLE]**
## Application Continuous Deployment (CI/CD)

---

### **[THE PIPELINE FLOW]**

```
[ DEVELOPER COMMITS SPRING BOOT / NEXT.JS CODE ]
                       ↓
         [ GITHUB ACTIONS RUNS TESTS ]
                       ↓
     [ BUILD DOCKER IMAGE & TAG VERSION (v1.x) ]
                       ↓
          [ PUSH DOCKER IMAGE TO AWS ECR ]
                       ↓
     [ DEPLOY NEW TASK DEFINITION ON ECS FARGATE ]
                       ↓ (Zero Downtime)
       [ HEALTH CHECKS PASS & TRAFFIC ROTATES ]
```

---

### **[WORKFLOW BENEFITS]**
*   **Docker Containerization**: Đảm bảo ứng dụng Spring Boot và Next.js chạy nhất quán trên mọi môi trường (Local, Staging, Production).
*   **Secure Registry (Amazon ECR)**: Lưu trữ ảnh Docker an toàn, tích hợp quét lỗ hổng bảo mật tự động trước khi triển khai.
*   **Zero-Downtime Deployment (ECS Fargate)**: ECS Fargate tự động khởi chạy Container mới, kiểm tra trạng thái sức khỏe (Healthcheck) trước khi ngắt kết nối với Container cũ, đảm bảo người xem phim không gặp bất kỳ gián đoạn nào.

---

## 🛝 Slide 8: The Power of Unified Data – Business Impact & Metrics (Dark Theme)

*   **Slide Theme**: Dark Mode (Deep Teal `#001e2b` Background)
*   **Layout**: Split 2x2 grid highlighting high-impact numbers and conclusions.
*   **Visual Elements**:
    *   Huge numbers styled in bold MongoDB Green (`#00ed64`).
    *   Contrast layout to finalize the pitch.

---

### 📝 Slide Copy (Vietnamese & English)

#### **[SLIDE EYEBROW]**
`BUSINESS IMPACT & PLATFORM CONCLUSION`

### **[SLIDE TITLE]**
## Tại sao lựa chọn MongoDB Atlas & AWS cho tương lai của Discovery?

---

### **[GRID DATA POINTS]**

#### 🟢 **-60% Architecture Complexity**
*   *Hợp nhất hạ tầng*: Loại bỏ hoàn toàn sự phân tách giữa Database nghiệp vụ và Database Vector. 1 Cụm MongoDB Atlas thay thế cho 4 giải pháp riêng lẻ.

#### 🟢 **< 100ms Latency**
*   *Phản hồi vượt trội*: Truy vấn ngữ nghĩa kết hợp với Pipeline xếp hạng thời gian thực hoạt động trực tiếp trong RAM của Atlas, đảm bảo trang chủ tải tức thì.

#### 🟢 **100% Serverless Scaling**
*   *Tự động hóa vận hành*: Kết hợp AWS ECS Fargate và MongoDB Atlas Serverless giúp hệ thống tự động co giãn theo lượng người truy cập mà không cần quản trị máy chủ vật lý.

#### 🟢 **Zero-Sync Data Integrity**
*   *Nhất quán dữ liệu tuyệt đối*: Không còn độ trễ đồng bộ (Sync lag) giữa hành vi người dùng và bộ gợi ý. Khách hàng Like phim nào, trang chủ đổi mới ngay lập tức.

---

# 🚀 Brainstorming Presentation Tips (How to deliver this to Judges)

1.  **Do not pitch a streaming clone**: Explain that this is an **Intelligence Discovery Platform** that can be integrated into *any* existing streaming catalog to boost user retention.
2.  **Highlight the code live if possible**: Show the judges how clean the MongoDB Vector Search query is. The simplicity of having database records and search vectors in the *same* document is a massive developer velocity story.
3.  **Frame the AWS Architecture as "Enterprise-Grade"**: Mention that utilizing GuardDuty, WAF, ALB, and ECS Fargate in a secure VPC proves your platform is ready to handle real-world scale and security standards immediately.
