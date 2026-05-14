# 失物招领系统

一个简单的基于 Spring Boot + MySQL + JPA 的失物招领管理系统。

## 功能特性

- ✅ 失物登记：在线登记丢失物品信息
- ✅ 失物查询：支持按名称、地点搜索
- ✅ REST API：提供完整的 CRUD 接口
- ✅ 响应式前端：美观的用户界面
- ✅ MySQL 持久化：数据永久存储

## 技术栈

- **后端**: Spring Boot 3.4.2
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA
- **前端**: HTML5 + CSS3 + JavaScript
- **模板引擎**: Thymeleaf

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

在 MySQL 中创建数据库：

```sql
CREATE DATABASE lost_found_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `src/main/resources/application.properties` 中的数据库配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/lost_found_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

访问 http://localhost:8082

## API 接口

### 获取所有失物
```
GET /api/lost-items
```

### 创建失物记录
```
POST /api/lost-items
Content-Type: application/json

{
  "itemName": "黑色钱包",
  "description": "内有身份证和银行卡",
  "location": "图书馆三楼",
  "contactPerson": "张三",
  "contactPhone": "13800138000",
  "lostTime": "2024-01-15T10:30:00",
  "status": "LOST"
}
```

### 搜索失物
```
GET /api/lost-items/search?name=钱包
GET /api/lost-items/search?location=图书馆
GET /api/lost-items/search?status=LOST
```

### 获取单个失物
```
GET /api/lost-items/{id}
```

### 更新失物
```
PUT /api/lost-items/{id}
Content-Type: application/json

{
  "itemName": "黑色钱包",
  "status": "FOUND"
}
```

### 删除失物
```
DELETE /api/lost-items/{id}
```

## 项目结构

```
langchain4j-spring-boot-07-lostFoundAssistant/
├── src/main/java/com/langchain4j/
│   ├── entity/
│   │   └── LostItem.java          # 失物实体类
│   ├── repository/
│   │   └── LostItemRepository.java # JPA Repository
│   ├── service/
│   │   └── LostItemService.java    # 业务逻辑层
│   ├── controller/
│   │   └── LostItemController.java # REST Controller
│   └── Application.java            # 启动类
├── src/main/resources/
│   ├── templates/
│   │   └── index.html              # 前端页面
│   ├── application.properties      # 配置文件
│   └── schema.sql                  # 数据库脚本
└── pom.xml
```

## 注意事项

1. 首次运行前请确保 MySQL 服务已启动
2. 根据实际环境修改数据库用户名和密码
3. JPA 会自动创建表结构（ddl-auto=update）
4. 前端页面支持响应式设计，可在移动端访问
