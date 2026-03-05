# 云图空间后端

## 📝 项目简介

云图空间后端是一个功能丰富的图片管理系统，支持图片上传、存储、审核、空间管理等功能。系统采用前后端分离架构，提供 RESTful API 接口，支持多用户协作和权限管理。

### 核心功能

- 🖼️ **图片管理**：上传、存储、编辑、删除图片，支持多种格式
- 🔍 **图片搜索**：基于标签、分类、名称等多维度搜索
- 👥 **用户系统**：用户注册、登录、权限管理（普通用户/管理员）
- 📦 **空间管理**：支持私有空间和团队空间，多级空间（普通版/专业版/旗舰版）
- 🤝 **协作管理**：空间成员管理，支持不同角色（viewer/editor/admin）
- ✅ **审核系统**：图片审核流程，支持通过/拒绝
- 🎨 **图片分析**：自动提取图片主色调、尺寸、宽高比等信息
- ☁️ **对象存储**：集成腾讯云 COS，支持大规模图片存储
- 🤖 **AI 集成**：集成阿里云 AI，支持智能图片分析

## 🛠️ 技术栈

### 核心框架
- **Spring Boot 2.7.6**：应用主框架
- **MyBatis-Plus 3.5.9**：数据库框架
- **MySQL**：关系型数据库
- **Redis**：缓存和 Session 管理

### 主要依赖
- **Sa-Token 1.39.0**：权限认证框架
- **Knife4j 4.4.0**：接口文档（Swagger UI 增强版）
- **Hutool 5.8.38**：Java 工具类库
- **Caffeine 3.1.8**：本地缓存
- **Jsoup 1.15.3**：HTML 解析
- **WebSocket**：实时通信
- **Disruptor 3.4.2**：高性能无锁队列

### 云服务集成
- **腾讯云 COS**：对象存储服务
- **阿里云 AI**：智能图片分析

## 📋 系统要求

- JDK 1.8 或更高版本
- Maven 3.x
- MySQL 5.7 或更高版本
- Redis 3.x 或更高版本

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd yun-picture-bakend
```

### 2. 数据库配置

#### 创建数据库
```bash
mysql -u root -p
```

执行 SQL 脚本：
```sql
source sql/create_table.sql
```

或者直接在 MySQL 中运行 `sql/create_table.sql` 文件中的 SQL 语句。

### 3. 配置文件

根据不同环境修改配置文件：

#### 本地开发环境
编辑 `src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yun_picture
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
    database: 2

# 腾讯云 COS 配置
cos:
  client:
    host: your-cos-host
    secretId: your-secret-id
    secretKey: your-secret-key
    region: your-region
    bucket: your-bucket

# 阿里云 AI 配置
aliYunAi:
  apiKey: your-api-key
```

#### 生产环境
编辑 `src/main/resources/application-prod.yml` 进行相应配置。

### 4. 构建项目

```bash
mvn clean install
```

### 5. 运行项目

```bash
mvn spring-boot:run
```

或者运行打包后的 JAR 文件：

```bash
java -jar target/yun-picture-bakend-0.0.1-SNAPSHOT.jar
```

### 6. 访问应用

- **应用地址**：http://localhost:8123
- **API 基础路径**：http://localhost:8123/api
- **接口文档**：http://localhost:8123/api/doc.html（Knife4j 文档）

## 📊 数据库设计

### 主要表结构

#### 用户表 (user)
- 用户账号、密码、昵称、头像
- 用户角色：user/admin
- 支持用户简介

#### 图片表 (picture)
- 图片 URL、缩略图 URL
- 图片基本信息：名称、分类、标签
- 图片属性：尺寸、宽度、高度、宽高比、格式、主色调
- 审核状态：待审核/通过/拒绝
- 关联用户和空间

#### 空间表 (space)
- 空间名称、级别（普通版/专业版/旗舰版）
- 空间类型：私有/团队
- 容量限制：最大总大小、最大数量
- 当前使用量：总大小、图片数量

#### 空间成员表 (space_user)
- 空间与用户关联
- 成员角色：viewer/editor/admin

## 🏗️ 项目结构

```
src/main/java/com/yupi/yupicturebackend/
├── annotation/          # 自定义注解（如权限校验）
├── aop/                # AOP 切面（如认证拦截器）
├── api/                # 第三方 API 集成
│   ├── aliyunai/      # 阿里云 AI
│   └── imagesearch/   # 图片搜索
├── common/             # 公共类（响应封装、分页等）
├── config/             # 配置类（跨域、COS 等）
├── constant/           # 常量定义
├── controller/         # 控制器层
│   ├── PictureController.java
│   ├── SpaceController.java
│   ├── SpaceUserController.java
│   ├── UserController.java
│   └── ...
├── exception/          # 异常处理
├── manage/             # 业务管理层（分片策略等）
├── mapper/             # MyBatis Mapper 接口
├── model/              # 数据模型（实体类、DTO、VO）
├── service/            # 服务层
└── utils/              # 工具类
```

## 🔐 权限管理

系统使用 Sa-Token 进行权限认证：

- **普通用户 (user)**：可上传、管理自己的图片，查看公共空间
- **管理员 (admin)**：拥有所有权限，可审核图片、管理所有用户和空间

### 空间权限

- **viewer（查看者）**：只能查看空间内容
- **editor（编辑者）**：可查看和编辑空间内容
- **admin（管理员）**：拥有空间的完全控制权

## 🎯 核心特性



### 1. 多级缓存
- **Redis**：分布式缓存，Session 共享
- **Caffeine**：本地缓存，提升热点数据访问速度

### 2. 异步处理
使用 `@EnableAsync` 支持异步任务处理，如图片分析、缩略图生成等。

### 3. 实时通信
集成 WebSocket，支持实时消息推送和协作功能。

### 4. 高性能队列
使用 Disruptor 实现高性能无锁队列，处理高并发场景。

## 📚 API 文档

启动项目后访问：http://localhost:8123/api/doc.html

使用 Knife4j 提供的 Swagger UI 增强版，可以：
- 查看所有接口文档
- 在线测试接口
- 查看请求/响应示例

## 🧪 测试

运行单元测试：

```bash
mvn test
```

测试类位于 `src/test/java/` 目录下。

## 📦 部署

### 打包

```bash
mvn clean package
```

生成的 JAR 文件位于 `target/yun-picture-bakend-0.0.1-SNAPSHOT.jar`

### 运行

```bash
java -jar target/yun-picture-bakend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker 部署（可选）

创建 `Dockerfile`：

```dockerfile
FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY target/yun-picture-bakend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

构建镜像：
```bash
docker build -t yun-picture-backend .
```

运行容器：
```bash
docker run -d -p 8123:8123 --name yun-picture-backend yun-picture-backend
```

## 🔧 配置说明

### 主要配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8123 |
| server.servlet.context-path | 应用上下文路径 | /api |
| spring.datasource.url | 数据库连接 | jdbc:mysql://localhost:3306/yun_picture |
| spring.redis.database | Redis 数据库索引 | 2 |
| spring.servlet.multipart.max-file-size | 最大上传文件大小 | 10MB |

### 环境切换

通过 `spring.profiles.active` 指定环境：
- `local`：本地开发环境
- `prod`：生产环境

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建新分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目仅供学习交流使用。

## 👨‍💻 作者

开发者：SampageGu

## 📞 联系方式

如有问题或建议，欢迎提交 Issue。

## ⚠️ 注意事项

1. **敏感信息安全**：请勿将 `application-local.yml` 中的密钥信息提交到公共仓库
2. **数据库备份**：生产环境使用前请做好数据库备份
3. **性能优化**：根据实际业务量调整分表策略和缓存配置
4. **监控告警**：生产环境建议配置应用监控和日志告警

---

⭐ 如果这个项目对你有帮助，请给个 Star！
