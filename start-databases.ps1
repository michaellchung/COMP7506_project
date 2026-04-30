# NoteMind 数据库启动脚本
# 启动 Milvus (向量数据库) + 准备 SQLite 目录

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  NoteMind Database Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker
Write-Host "[1/3] 检查 Docker 状态..." -ForegroundColor Yellow
try {
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Docker 未运行，请先启动 Docker Desktop" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Docker 运行正常" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker 命令失败，请确保 Docker Desktop 已安装并运行" -ForegroundColor Red
    exit 1
}

# 创建 SQLite 目录（如果不存在）
Write-Host "[2/3] 准备 SQLite 目录..." -ForegroundColor Yellow
$sqliteDir = Join-Path $PSScriptRoot "backend"
if (-not (Test-Path $sqliteDir)) {
    New-Item -ItemType Directory -Path $sqliteDir | Out-Null
    Write-Host "✅ 创建目录: $sqliteDir" -ForegroundColor Green
} else {
    Write-Host "✅ SQLite 目录已存在" -ForegroundColor Green
}

# 启动 Docker Compose
Write-Host "[3/3] 启动 Milvus 数据库..." -ForegroundColor Yellow
Write-Host "    这可能需要几分钟（首次启动会下载镜像）..." -ForegroundColor Gray

docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker Compose 启动失败" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ 所有数据库已启动！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📊 服务状态:" -ForegroundColor Cyan
Write-Host "   • Milvus:     localhost:19530 (向量数据库)" -ForegroundColor White
Write-Host "   • Minio:      localhost:9000  (对象存储控制台)" -ForegroundColor White
Write-Host "   • SQLite:     ./backend/notemind.db (文件型，自动创建)" -ForegroundColor White
Write-Host ""
Write-Host "🔧 常用命令:" -ForegroundColor Cyan
Write-Host "   查看日志: docker-compose logs -f milvus-standalone" -ForegroundColor Gray
Write-Host "   停止服务: docker-compose down" -ForegroundColor Gray
Write-Host "   完全清理: docker-compose down -v" -ForegroundColor Gray
Write-Host ""
Write-Host "⚠️  注意: Milvus 启动需要约 30-60 秒，请稍等再启动后端" -ForegroundColor Yellow
Write-Host ""
