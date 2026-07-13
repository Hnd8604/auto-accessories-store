# 🛍️ Store Backend API

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A comprehensive e-commerce backend API built with Spring Boot, featuring authentication, product management, shopping cart, order processing, and payment integration.

[English](#english) | [Tiếng Việt](#tiếng-việt)

---

## English

### 📋 Table of Contents
- [Features](#features)
- [Technologies](#technologies)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

### ✨ Features

#### 🔐 Authentication & Authorization
- JWT-based authentication (access & refresh tokens)
- Google OAuth2 integration
- Role-based access control (RBAC) with permissions
- Password reset with OTP verification
- Email notifications
- Token introspection and validation
- Session management

#### 🛒 E-commerce Core
- **Product Management**: CRUD operations with image gallery support
- **Category & Brand Management**: Hierarchical organization
- **Shopping Cart**: 
  - Session-based cart for guest users
  - Persistent cart for authenticated users
- **Order Management**: Complete order lifecycle tracking
- **Payment Integration**: SePay payment gateway support
- **Inventory Control**: Stock tracking and management

#### 📝 Content Management
- Blog/Post system with categories
- Banner management for promotions
- Rich content support

#### 🚀 Technical Features
- Redis caching for improved performance
- Cloudinary integration for image storage and optimization
- Email service (SMTP)
- RESTful API design
- JPA auditing for entity tracking
- Comprehensive validation
- Error handling and custom exceptions
- API documentation with Swagger/OpenAPI

### 🛠️ Technologies

| Category | Technologies |
|----------|-------------|
| **Framework** | Spring Boot 3.4.5 |
| **Language** | Java 21 |
| **Database** | MySQL 8.0+ |
| **Cache** | Redis |
| **Security** | Spring Security, OAuth2 Resource Server, JWT |
| **ORM** | Spring Data JPA, Hibernate |
| **Documentation** | SpringDoc OpenAPI (Swagger) |
| **Build Tool** | Maven |
| **Object Mapping** | MapStruct |
| **Code Simplification** | Lombok |
| **Cloud Storage** | Cloudinary |
| **Email** | Spring Mail |
| **Validation** | Jakarta Validation |

### 📦 Prerequisites

Before running this application, ensure you have the following installed:

- **Java Development Kit (JDK) 21** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

### 🚀 Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd store-be
```

2. **Create MySQL database**
```sql
CREATE DATABASE store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **Configure application properties**
```bash
# Copy the example configuration file
cp src/main/resources/application-example.yaml src/main/resources/application.yaml

# Edit application.yaml with your actual values
```

4. **Install dependencies**
```bash
./mvnw clean install
```

### ⚙️ Configuration

Edit `src/main/resources/application.yaml` with your configuration:

#### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/store
    username: your_db_username
    password: your_db_password
```

#### Redis Configuration
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
```

#### JWT Configuration
```yaml
jwt:
  signerKey: "your_jwt_signer_key_base64"  # Generate: openssl rand -base64 48
  access-duration: 3600                     # 1 hour
  refresh-duration: 2592000                 # 30 days
```

#### Cloudinary Configuration
```yaml
cloudinary:
  cloud-name: your_cloud_name
  api-key: your_cloudinary_api_key
  api-secret: your_cloudinary_api_secret
```

#### Email Configuration
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password  # Google App Password (16 characters)
```

#### Payment Gateway (SePay)
```yaml
sepay:
  api-key: "YOUR_SEPAY_API_KEY"
  bank-code: "MBBank"
  bank-account-number: "0123456789"
  bank-account-name: "NGUYEN VAN A"
```

#### Google OAuth2
```yaml
google:
  client-id: "YOUR_GOOGLE_CLIENT_ID"
  client-secret: "YOUR_GOOGLE_CLIENT_SECRET"
  redirect-uri: "http://localhost:5173/auth/google/callback"
```

### 🏃 Running the Application

#### Using Maven Wrapper
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

#### Using Maven (if installed globally)
```bash
mvn spring-boot:run
```

#### Using JAR
```bash
# Build the JAR
./mvnw clean package

# Run the JAR
java -jar target/store-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080/api/v1`

### 📚 API Documentation

Once the application is running, access the Swagger UI documentation at:

```
http://localhost:8080/api/v1/swagger-ui.html
```

Or access the OpenAPI JSON specification:

```
http://localhost:8080/api/v1/v3/api-docs
```

### 📁 Project Structure

```
store-be/
├── src/
│   ├── main/
│   │   ├── java/app/store/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── CloudinaryConfig.java
│   │   │   │   └── SwaggerConfig.java
│   │   │   ├── controller/          # REST API controllers
│   │   │   │   ├── AuthenticationController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   └── ...
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/              # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Order.java
│   │   │   │   └── ...
│   │   │   ├── repository/          # JPA repositories
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── impl/
│   │   │   │   └── interfaces/
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── validator/           # Custom validators
│   │   │   ├── utils/               # Utility classes
│   │   │   ├── enums/               # Enumerations
│   │   │   ├── specification/       # JPA specifications
│   │   │   └── seed/                # Database seeders
│   │   └── resources/
│   │       └── application.yaml     # Application configuration
│   └── test/                        # Test classes
├── pom.xml                          # Maven configuration
└── README.md
```

### 🔒 Default Credentials

After initial setup, the application creates default admin credentials (configured in `ApplicationInitConfig.java`):

Check the seeder files in `src/main/java/app/store/seed/` for initial data.

### 🌐 CORS Configuration

CORS is configured to allow requests from your frontend application. Update `CorsConfig.java` to match your frontend URL.

### 📝 Additional Notes

- The application uses JPA auditing for automatic tracking of created/updated dates
- MapStruct is used for efficient DTO-Entity mapping
- Redis is used for caching frequently accessed data and storing invalidated tokens
- All endpoints except public ones require JWT authentication
- File uploads are handled through Cloudinary
- Payment webhooks are available for SePay integration

### 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 📄 License

This project is licensed under the MIT License.

---

## Tiếng Việt

### 📋 Mục lục
- [Tính năng](#tính-năng)
- [Công nghệ](#công-nghệ)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Cấu hình](#cấu-hình)
- [Chạy ứng dụng](#chạy-ứng-dụng)
- [Tài liệu API](#tài-liệu-api)
- [Cấu trúc dự án](#cấu-trúc-dự-án)

### ✨ Tính năng

#### 🔐 Xác thực & Phân quyền
- Xác thực dựa trên JWT (access & refresh tokens)
- Tích hợp đăng nhập Google OAuth2
- Phân quyền dựa trên vai trò (RBAC) với permissions
- Đặt lại mật khẩu với xác thực OTP
- Thông báo qua email
- Kiểm tra và xác thực token
- Quản lý phiên đăng nhập

#### 🛒 Chức năng Thương mại điện tử
- **Quản lý Sản phẩm**: CRUD với hỗ trợ thư viện ảnh
- **Quản lý Danh mục & Thương hiệu**: Tổ chức phân cấp
- **Giỏ hàng**: 
  - Giỏ hàng tạm thời cho khách vãng lai
  - Giỏ hàng lưu trữ cho người dùng đã đăng nhập
- **Quản lý Đơn hàng**: Theo dõi toàn bộ vòng đời đơn hàng
- **Tích hợp Thanh toán**: Hỗ trợ cổng thanh toán SePay
- **Kiểm soát Tồn kho**: Theo dõi và quản lý hàng tồn kho

#### 📝 Quản lý Nội dung
- Hệ thống Blog/Bài viết với danh mục
- Quản lý Banner cho khuyến mãi
- Hỗ trợ nội dung phong phú

#### 🚀 Tính năng Kỹ thuật
- Bộ nhớ đệm Redis để cải thiện hiệu suất
- Tích hợp Cloudinary cho lưu trữ và tối ưu hóa hình ảnh
- Dịch vụ email (SMTP)
- Thiết kế RESTful API
- JPA auditing để theo dõi thực thể
- Xác thực toàn diện
- Xử lý lỗi và exceptions tùy chỉnh
- Tài liệu API với Swagger/OpenAPI

### 🛠️ Công nghệ

| Danh mục | Công nghệ |
|----------|-----------|
| **Framework** | Spring Boot 3.4.5 |
| **Ngôn ngữ** | Java 21 |
| **Cơ sở dữ liệu** | MySQL 8.0+ |
| **Cache** | Redis |
| **Bảo mật** | Spring Security, OAuth2 Resource Server, JWT |
| **ORM** | Spring Data JPA, Hibernate |
| **Tài liệu** | SpringDoc OpenAPI (Swagger) |
| **Build Tool** | Maven |
| **Object Mapping** | MapStruct |
| **Đơn giản hóa Code** | Lombok |
| **Cloud Storage** | Cloudinary |
| **Email** | Spring Mail |
| **Validation** | Jakarta Validation |

### 📦 Yêu cầu hệ thống

Trước khi chạy ứng dụng, đảm bảo bạn đã cài đặt:

- **Java Development Kit (JDK) 21** trở lên
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **IDE** (Khuyên dùng IntelliJ IDEA, Eclipse, hoặc VS Code)

### 🚀 Cài đặt

1. **Clone repository**
```bash
git clone <repository-url>
cd store-be
```

2. **Tạo cơ sở dữ liệu MySQL**
```sql
CREATE DATABASE store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **Cấu hình application properties**
```bash
# Copy file cấu hình mẫu
cp src/main/resources/application-example.yaml src/main/resources/application.yaml

# Chỉnh sửa application.yaml với giá trị thực của bạn
```

4. **Cài đặt dependencies**
```bash
./mvnw clean install
```

### ⚙️ Cấu hình

Chỉnh sửa `src/main/resources/application.yaml` với cấu hình của bạn:

#### Cấu hình Cơ sở dữ liệu
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/store
    username: tên_người_dùng_db
    password: mật_khẩu_db
```

#### Cấu hình Redis
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: mật_khẩu_redis
```

#### Cấu hình JWT
```yaml
jwt:
  signerKey: "khóa_ký_jwt_base64"  # Tạo bằng: openssl rand -base64 48
  access-duration: 3600             # 1 giờ
  refresh-duration: 2592000         # 30 ngày
```

#### Cấu hình Cloudinary
```yaml
cloudinary:
  cloud-name: tên_cloud_của_bạn
  api-key: api_key_cloudinary
  api-secret: api_secret_cloudinary
```

#### Cấu hình Email
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: email_của_bạn@gmail.com
    password: mật_khẩu_ứng_dụng  # Google App Password (16 ký tự)
```

#### Cổng thanh toán (SePay)
```yaml
sepay:
  api-key: "SEPAY_API_KEY_CỦA_BẠN"
  bank-code: "MBBank"
  bank-account-number: "0123456789"
  bank-account-name: "NGUYEN VAN A"
```

#### Google OAuth2
```yaml
google:
  client-id: "GOOGLE_CLIENT_ID_CỦA_BẠN"
  client-secret: "GOOGLE_CLIENT_SECRET_CỦA_BẠN"
  redirect-uri: "http://localhost:5173/auth/google/callback"
```

### 🏃 Chạy ứng dụng

#### Sử dụng Maven Wrapper
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

#### Sử dụng Maven (nếu đã cài đặt global)
```bash
mvn spring-boot:run
```

#### Sử dụng JAR
```bash
# Build JAR
./mvnw clean package

# Chạy JAR
java -jar target/store-0.0.1-SNAPSHOT.jar
```

Ứng dụng sẽ khởi động tại `http://localhost:8080/api/v1`

### 📚 Tài liệu API

Sau khi ứng dụng chạy, truy cập Swagger UI tại:

```
http://localhost:8080/api/v1/swagger-ui.html
```

Hoặc truy cập OpenAPI JSON specification:

```
http://localhost:8080/api/v1/v3/api-docs
```

### 📁 Cấu trúc dự án

```
store-be/
├── src/
│   ├── main/
│   │   ├── java/app/store/
│   │   │   ├── config/              # Các class cấu hình
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── CloudinaryConfig.java
│   │   │   │   └── SwaggerConfig.java
│   │   │   ├── controller/          # REST API controllers
│   │   │   │   ├── AuthenticationController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   └── ...
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/              # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Order.java
│   │   │   │   └── ...
│   │   │   ├── repository/          # JPA repositories
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── impl/
│   │   │   │   └── interfaces/
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── validator/           # Custom validators
│   │   │   ├── utils/               # Utility classes
│   │   │   ├── enums/               # Enumerations
│   │   │   ├── specification/       # JPA specifications
│   │   │   └── seed/                # Database seeders
│   │   └── resources/
│   │       └── application.yaml     # Cấu hình ứng dụng
│   └── test/                        # Test classes
├── pom.xml                          # Cấu hình Maven
└── README.md
```

### 🔒 Tài khoản Mặc định

Sau khi cài đặt ban đầu, ứng dụng tạo tài khoản admin mặc định (được cấu hình trong `ApplicationInitConfig.java`):

Kiểm tra các file seeder trong `src/main/java/app/store/seed/` để biết dữ liệu khởi tạo.

### 🌐 Cấu hình CORS

CORS được cấu hình để chấp nhận requests từ frontend của bạn. Cập nhật `CorsConfig.java` để khớp với URL frontend.

### 📝 Ghi chú Bổ sung

- Ứng dụng sử dụng JPA auditing để tự động theo dõi ngày tạo/cập nhật
- MapStruct được sử dụng để ánh xạ DTO-Entity hiệu quả
- Redis được sử dụng để cache dữ liệu truy cập thường xuyên và lưu trữ các token đã vô hiệu hóa
- Tất cả endpoints trừ các endpoints công khai đều yêu cầu xác thực JWT
- Upload file được xử lý thông qua Cloudinary
- Webhook thanh toán có sẵn cho tích hợp SePay

### 🤝 Đóng góp

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/TinhNangTuyetVoi`)
3. Commit thay đổi (`git commit -m 'Thêm tính năng tuyệt vời'`)
4. Push lên branch (`git push origin feature/TinhNangTuyetVoi`)
5. Tạo Pull Request

### 📄 Giấy phép

Dự án này được cấp phép theo giấy phép MIT.

---

## 🔗 Liên hệ & Hỗ trợ

Nếu bạn có bất kỳ câu hỏi hoặc vấn đề nào, vui lòng tạo issue trên GitHub repository.

**Chúc bạn code vui vẻ! 🚀**
