# syntax=docker/dockerfile:1

# ==============================================================
# Backend image - multi-stage, layered jar, chạy bằng user non-root.
#
# Build tại thư mục backend/:
#   docker build -t store-backend .
#
# Toàn bộ cấu hình đọc từ biến môi trường (xem .env.example),
# không có secret nào nằm trong image.
# ==============================================================

# ---- Stage 1: Build ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy pom trước để layer dependency được cache lại khi chỉ src thay đổi
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Tách fat jar (~100MB) thành từng layer. dependencies/ gần như không đổi
# giữa các lần build nên deploy sau chỉ phải push/pull layer application/
# vài MB thay vì cả 100MB.
RUN cp target/*.jar app.jar && \
    java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# tzdata để TZ=Asia/Ho_Chi_Minh có hiệu lực, curl cho HEALTHCHECK
RUN apk add --no-cache tzdata curl && \
    addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# application.yaml ghi log ra ${LOG_PATH}, mặc định ../logs/backend -> tính
# từ WORKDIR /app thành /logs/backend, user non-root không tạo được thư mục
# ở root nên app chết ngay lúc khởi động. Trỏ về /app/logs và cấp quyền.
ENV LOG_PATH=/app/logs
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy theo thứ tự "ít thay đổi -> hay thay đổi" để tận dụng cache Docker
COPY --from=builder --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/application/ ./

USER appuser

# SPRING_PROFILES_ACTIVE để ở ENV (không phải -D) để docker-compose ghi đè được
ENV TZ=Asia/Ho_Chi_Minh \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD curl -fsS http://127.0.0.1:8080/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

# exec để java làm PID 1 -> nhận SIGTERM và shutdown gọn gàng khi restart
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -cp . org.springframework.boot.loader.launch.JarLauncher"]
