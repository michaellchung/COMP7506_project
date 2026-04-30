# NoteMind 快速启动指南

## 一键启动（推荐）

确保 **Docker Desktop** 已运行，手机通过 **USB 线**连接电脑并开启调试模式，然后执行：

```powershell
# Windows PowerShell
.\start-all.ps1

# 如果使用 macOS / Linux，请手动执行以下步骤：
# 1. docker-compose up -d
# 2. adb reverse tcp:5000 tcp:5000
# 3. cd backend && source .venv/bin/activate && python app.py
```

这会同时完成三件事：
1. ✅ 启动 Milvus 向量数据库（Docker）
2. ✅ 配置 ADB 端口映射（手机 → PC）
3. ✅ 启动 Python Flask 后端（独立窗口）

等待显示 `"✅ 启动流程完成！"` 即可。

---

## 分步启动（如果需要单独控制）

### 1. 只启动数据库
```powershell
.\start-all.ps1 -SkipBackend -SkipAdb
```

### 2. 只配置 ADB（数据库已运行）
```powershell
.\start-all.ps1 -SkipDocker -SkipBackend
```

### 3. 只启动后端（数据库和 ADB 已就绪）
```powershell
.\start-all.ps1 -SkipDocker -SkipAdb
```

---

## 首次使用检查清单

### 电脑上
- [ ] Docker Desktop 已安装并运行
- [ ] Python 3.10+ 已安装
- [ ] 后端虚拟环境已创建（脚本会自动创建）

### 手机上
- [ ] 开启开发者选项 → USB 调试
- [ ] 用数据线连接电脑
- [ ] 授权调试弹窗点击"允许"
- [ ] 安装 NoteMind App（Android Studio 运行）

---

## 连接配置

启动完成后，在 App 内设置：

| 设置项 | 值 | 说明 |
|:---|:---|:---|
| Profile → Backend URL | `http://127.0.0.1:5000` | 通过 ADB 映射 |
| Profile → Display Name | 任意 | 显示在头像上 |

点击"Save Changes"保存，然后进入 Chat 页面发送消息测试。

---

## 常见错误

### ❌ "无法识别 adb"

脚本会自动查找常见路径。如果失败，手动添加环境变量：

```powershell
# Windows PowerShell - 请根据实际 SDK 安装路径调整
$env:PATH += ";$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools"

# macOS / Linux - 添加到 ~/.bashrc 或 ~/.zshrc
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
```

### ❌ "Docker 未运行"
启动 Docker Desktop 后再试。

### ❌ "Milvus 连接超时"
Milvus 首次启动需要 1-2 分钟下载镜像。再次运行脚本即可。

### ❌ "手机无法连接"
- 检查 USB 线是否支持数据传输（有些只充电）
- 手机上重新授权调试
- 换 USB 端口试试

---

## 停止服务

```bash
# 停止 Docker 数据库（会保留数据）
docker-compose down

# 完全删除数据库（包括向量数据）
docker-compose down -v
rm backend/notemind.db    # macOS / Linux
# 或: Remove-Item backend/notemind.db    # Windows PowerShell

# 停止后端
# 直接关闭后端终端窗口即可
```
