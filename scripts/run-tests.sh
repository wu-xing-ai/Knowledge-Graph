#!/usr/bin/env bash
# 一键运行前后端自动化测试
# 用法: ./scripts/run-tests.sh

set -e

echo "=== 运行后端测试 ==="
cd backend
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
mvn test
cd ..

echo ""
echo "=== 运行前端测试 ==="
cd frontend
npm test
cd ..

echo ""
echo "=== 全部测试通过 ==="
