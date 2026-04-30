# NoteMind 数据库设置指南

## 架构概览

NoteMind 使用两个数据库：

| 数据库 | 用途 | 数据类型 | 运行方式 |
|:---|:---|:---|:---|
| **SQLite** | 业务数据存储 | 会话、消息、笔记、API用量 | 文件型，无需容器 |
| **Milvus** | 向量检索 | 文本分块的 Embedding 向量 | Docker 容器 |

## 快速启动（推荐）

### 1. 一键启动

在 PowerShell 中执行：

```powershell
cd C:\Users\Michael\AndroidStudioProjects\COMP7506_project
.\start-databases.ps1
```

或手动用 Docker Compose：

```powershell
docker-compose up -d
```

### 2. 等待 Milvus 就绪

首次启动需要下载镜像，约 2-5 分钟：

```powershell
# 查看日志，看到 "[INFO] [rootcoord/root_coord.go]..." 表示启动完成
docker-compose logs -f milvus-standalone
```

### 3. 启动后端

```powershell
cd backend
.venv\Scripts\activate
python app.py
```

## 手动分步说明

### Milvus（向量数据库）

```powershell
# 启动所有服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs milvus-standalone

# 停止
docker-compose down

# 完全删除数据（包括所有向量）
docker-compose down -v
```

### SQLite（无需操作）

SQLite 是文件型数据库，后端启动时会自动创建 `backend/notemind.db` 文件。

## 连接配置

### 后端 `.env` 配置

```env
# Milvus 连接（使用 Docker 默认端口）
MILVUS_HOST=localhost
MILVUS_PORT=19530
EMBEDDING_DIM=1536

# SQLite 无需配置，自动创建
```

### 验证连接

启动后端后，访问健康检查：

```bash
curl http://localhost:5000/health
# 应返回: {"status": "ok", "service": "NoteMind backend"}
```

## 故障排查

### ❌ "Cannot connect to Milvus"

1. 检查容器状态：`docker-compose ps`
2. 查看日志：`docker-compose logs milvus-standalone`
3. 等待完全启动：Milvus 需要 30-60 秒初始化
4. 检查端口占用：`netstat -ano | findstr 19530`

### ❌ "Connection refused" 从手机连接

确保：
1. 后端 `app.py` 的 `host="0.0.0.0"`（监听所有接口）
2. 手机通过 `adb reverse` 或局域网 IP 连接
3. Windows 防火墙放行 5000 端口

### ❌ Docker 启动失败

确保 Docker Desktop：
- 已安装且正在运行
- 内存分配 >= 4GB（Milvus 需要）
- Settings → Resources → Memory 调高到 4GB+

## 服务端口对照

| 服务 | 端口 | 访问方式 |
|:---|:---|:---|
| Milvus gRPC | 19530 | 后端代码连接 |
| Milvus HTTP | 9091 | 调试接口 |
| Minio 控制台 | 9001 | http://localhost:9001 (minioadmin/minioadmin) |
| Minio API | 9000 | Milvus 内部使用 |
| Flask 后端 | 5000 | http://localhost:5000 |

## 数据持久化

- **Milvus 数据**：Docker Volume `milvus-data` 自动持久化
- **SQLite 数据**：`backend/notemind.db` 文件，随项目保存

删除数据：
```powershell
# 仅删 Milvus 向量数据
docker-compose down -v

# 删 SQLite 数据（会丢失所有会话和笔记）
Remove-Item backend/notemind.db
```
