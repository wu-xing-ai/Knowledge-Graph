# ============ 知联汇学 后端镜像（多阶段构建）============
# 阶段 1：Maven 构建
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# 预下载依赖，充分利用缓存
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests

# 阶段 2：精简运行镜像（只带 JRE）
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/zhi-platform-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]