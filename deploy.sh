#!/bin/bash

# 参数定义
FRONTEND_DIR="./web"
FRONTEND_BUILD_CMD="npm run build"
NODE_IMAGE="node:22-alpine"
DIST_SRC="$FRONTEND_DIR/dist"
DIST_DEST="./src/main/resources/dist"
BACKEND_JAR_PATH="./build/libs"
JAR_NAME="hmby-0.0.1-SNAPSHOT.jar"
GRADLE_CMD="./gradlew clean bootJar -x test"
JAVA_PROCESS_KEYWORD="hmby"
JVM_OPTS="-Xmx512m"

# Step 1: 编译前端项目
echo "👉 使用 Docker 编译前端项目..."
docker run --rm -v "$PWD/$FRONTEND_DIR":/app -w /app $NODE_IMAGE sh -c "npm install && $FRONTEND_BUILD_CMD"
if [ $? -ne 0 ]; then
  echo "❌ 前端编译失败，退出。"
  exit 1
fi
echo "✅ 前端编译完成。"

# Step 2: 拷贝前端构建产物
echo "👉 拷贝构建产物到 Spring Boot 资源目录..."
rm -rf "$DIST_DEST"
mkdir -p "$DIST_DEST"
cp -r "$DIST_SRC/"* "$DIST_DEST/"
if [ $? -ne 0 ]; then
  echo "❌ 拷贝前端构建产物失败，退出。"
  exit 1
fi
echo "✅ 构建产物已拷贝到 $DIST_DEST。"

# Step 3: 编译后端项目
echo "👉 编译后端 Gradle 项目..."
$GRADLE_CMD
if [ $? -ne 0 ]; then
  echo "❌ 后端构建失败，退出。"
  exit 1
fi
echo "✅ 后端构建完成。"

# Step 4: 使用 systemctl 启动 Java 服务
echo "👉 使用 systemctl 检查并重启 Java 服务..."

SERVICE_NAME="hmby"

# 检查服务是否已存在并运行
if systemctl is-active --quiet $SERVICE_NAME; then
  echo "🛑 Java 服务已在运行，正在重启..."
  sudo systemctl restart $SERVICE_NAME
else
  echo "✅ Java 服务未在运行，正在启动..."
  sudo systemctl start $SERVICE_NAME
fi

# 查看服务状态
sudo systemctl status $SERVICE_NAME --no-pager
