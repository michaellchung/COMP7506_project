# NoteMind

一款 AI 驱动的智能学习助手，帮助用户高效管理课程笔记、实现知识库问答与智能内容分析。

---

## 初版功能概览

### 1. 课程管理与笔记记录

- **添加课程**：在主页创建课程文件夹，直观管理不同学科
- **课时记录**：进入课程后可创建第 N 堂课，支持多维度内容采集
  - 录音功能：录制课堂音频并自动转录总结
  - 拍照 OCR：拍摄板书或文档，提取文字内容
  - 图片上传：从相册选择图片进行 OCR 解析
  - 课件解析：上传 PPT/PDF 文件，自动提取并总结内容
- **智能入库**：后端自动对内容进行分块、Embedding 向量化，存储至 Milvus 向量数据库

> **待改进**：
> - 录音无法实时显示转录内容
> - OpenRouter 音频大模型存在代理问题，转录稳定性欠佳，建议后续改用 OpenAI Whisper API

---

### 2. AI 知识库问答系统

- **快速入口**：主页右下角悬浮按钮，点击全屏进入对话界面
- **课程内问答入口**：课程详情页提供聊天按钮，进入后自动携带当前课程范围
- **会话管理**：左侧边栏支持新建会话、切换历史会话、长按删除会话
- **RAG 检索**：基于向量数据库实现检索增强生成，回答结合课程笔记内容
- **课程级知识库隔离**：上传内容入库时会关联 `course_id` / `lecture_id`，问答检索时优先按课程或课时过滤，避免不同课程资料混合检索
- **SQLite 降级检索**：Milvus 不可用时，后端会退回 SQLite 笔记检索，并继续按课程 / 课时范围过滤

> **待改进**：
> - 缺乏模型熔断机制，单一模型故障时无法自动降级
> - 未对用户问题进行意图分析，无法区分闲聊与知识查询

---

### 3. 用户账户系统（登录 / 注册）

- **登录 / 注册**：启动 App 自动跳转登录页，支持邮箱注册与登录
- **Token 鉴权**：注册 / 登录成功后颁发 UUID Token，本地持久化存储，重启 App 无需重新登录
- **退出登录**：Profile 页提供 Sign Out 按钮，清除本地 Token 并返回登录页
- **账户信息**：显示当前用户名、邮箱及后端 URL 配置
- **API 用量监控**：通过进度条直观展示 Token 消耗情况

> **待改进**：
> - 支持头像上传与更换
> - 密码重置 / 找回功能

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 移动端 | Android (Java), Material Design |
| 后端 | Python, Flask, SQLite |
| AI / LLM | OpenRouter API (GPT-4o, GPT-4o-mini), OpenAI Embedding |
| 向量数据库 | Milvus（支持按 `course_id` / `lecture_id` 进行范围过滤） |
| 部署 | Docker Compose (Milvus + etcd + MinIO) |

---

## 课程级 RAG 检索流程

1. 用户在某门课程的课时中录音、拍照 OCR、上传图片或上传 PPT/PDF。
2. 后端将处理结果保存为笔记，并从 `lecture_id` 反查所属 `course_id`。
3. 文本被分块后生成 Embedding，并与 `source_type`、`note_id`、`course_id`、`lecture_id` 一起写入 Milvus。
4. 用户从课程详情页进入 AI Chat 时，Android 客户端会把 `course_id` 传给 `/api/kb/ask`。
5. 后端向量检索时使用课程 / 课时过滤条件，只把当前范围内的笔记片段提供给大模型生成答案。
6. 如果 Milvus 不可用，则使用 SQLite fallback，但仍按 `course_id` / `lecture_id` 过滤，避免回到全局混合检索。

---

## 快速开始

### 后端启动

```bash
cd backend
pip install -r requirements.txt
python app.py
```

或使用一键启动脚本（Windows PowerShell，含 Docker、ADB 端口映射）：

```powershell
.\start-all.ps1
```

> **macOS / Linux 用户**：请手动执行 `docker-compose up -d` + `adb reverse tcp:5000 tcp:5000` + `python app.py`

### Android 客户端

1. 使用 Android Studio 打开项目
2. 确保已通过 ADB 连接设备：`adb reverse tcp:5000 tcp:5000`
3. 运行应用

---

## 项目结构

```
COMP7506_project/
├── app/                    # Android 客户端
│   ├── src/main/java/      # Java 源码
│   └── src/main/res/       # 布局与资源
├── backend/                # Flask 后端
│   ├── app.py              # 主入口
│   ├── services/           # 业务服务 (OCR, 转录, Embedding, RAG)
│   └── database.py         # SQLite 数据层
├── docker-compose.yml      # Milvus 依赖部署
└── README.md
```

---

## 后续规划

- [ ] 录音实时转录与字幕显示
- [ ] 接入 OpenAI Whisper 替代现有音频模型
- [ ] 多模型熔断与自动降级机制
- [ ] 用户意图识别与路由
- [x] 课程级知识库隔离
- [x] 用户账户系统（登录 / 注册 / Token 鉴权 / 退出登录）

---

> 本项目为 HKU COMP7506 课程项目初版实现。
