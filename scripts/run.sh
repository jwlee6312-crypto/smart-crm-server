#!/bin/bash
# 🚀 Smart ERP - Repaired Start Script
PROJECT_ROOT="/home/smart/smart-erp"

echo "=========================================="
echo "   Smart ERP System Repair Start          "
echo "=========================================="

# 1. 경로 이동
cd $PROJECT_ROOT

# 2. 필수 권한 보정 (사용자 요구사항 반영)
echo "[1/3] Fixing sound directory permissions..."
sudo chmod -R 777 /var/lib/asterisk/sounds/custom

# 3. 꼬인 네트워크 및 컨테이너 정리
echo "[2/3] Cleaning up Docker resources..."
sudo docker compose down

# 4. 통합 구축 시스템 전체 기동
echo "[3/3] Launching all services..."
sudo docker compose up -d

echo "=========================================="
echo " ✅ REPAIR COMPLETE: System is Up!       "
echo "=========================================="
