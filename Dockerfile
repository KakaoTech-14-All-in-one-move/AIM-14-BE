# Build 단계: 적절한 Java 21 빌드 이미지 사용
FROM eclipse-temurin:21-jdk-alpine as builder

# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트 파일 복사
COPY . .

# Gradle 캐시를 활용해 의존성 설치
RUN ./gradlew --no-daemon bootJar -x test

# Runtime 단계: 더 작은 JRE 이미지를 사용
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 시작
CMD ["java", "-jar", "app.jar"]