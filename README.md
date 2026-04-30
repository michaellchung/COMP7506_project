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
- **会话管理**：左侧边栏支持新建会话、切换历史会话、长按删除会话
- **RAG 检索**：基于向量数据库实现检索增强生成，回答结合课程笔记内容

> **待改进**：
> - 缺乏模型熔断机制，单一模型故障时无法自动降级
> - 未对用户问题进行意图分析，无法区分闲聊与知识查询
> - 知识库未按课程隔离，所有内容混合检索，精准度受限

---

### 3. 用户 Profile

- **账户信息**：显示用户基本信息与设置
- **API 用量监控**：通过进度条直观展示 Token 消耗情况

> **待改进**：
> - 界面较为简陋，可优化视觉设计
> - 缺少头像更换、登录系统等账户功能

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 移动端 | Android (Java), Material Design |
| 后端 | Python, Flask, SQLite |
| AI / LLM | OpenRouter API (GPT-4o, GPT-4o-mini), OpenAI Embedding |
| 向量数据库 | Milvus |
| 部署 | Docker Compose (Milvus + etcd + MinIO) |

---

## 快速开始

### 后端启动

```bash
cd backend
pip install -r requirements.txt
python app.py
```

或使用一键启动脚本（含 Docker、ADB 端口映射）：

```powershell
./start-all.ps1
```

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
- [ ] 课程级知识库隔离与权限控制
- [ ] 用户账户系统（登录/注册/头像）

---

> 本项目为 HKU COMP7506 课程项目初版实现。
