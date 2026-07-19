# Spring Boot WebMVC Demo

基于 Spring Boot 3.3.5 + JDK 21 的 WebMVC 示例项目。

## 技术栈

- JDK 21
- Spring Boot 3.3.5
- Spring Data JPA
- MySQL
- Lombok

## 项目结构

```
src/main/java/com/example/demo/
├── DemoApplication.java         # 主启动类
├── config/                      # 配置类
├── controller/                  # 控制器
├── service/                     # 服务层
├── repository/                  # 数据访问层
├── entity/                      # 实体类
├── dto/                         # 数据传输对象
└── exception/                   # 异常处理
```

## 快速开始

### 前置条件

- JDK 21
- Maven 3.8+
- MySQL 8.0+

### 配置数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

根据实际情况修改 `src/main/resources/application.yml` 中的数据库连接信息。

### 编译运行

```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run
```

服务启动后访问 http://localhost:8080

### 接口

- 健康检查：`GET /api/health`
