#!/bin/bash
PROJECT_ROOT="/home/smart/smart-erp-crm"
echo "=================================================="
echo "   Smart ERP CRM System Start (172.30.1.11 @ 3308)"
echo "=================================================="
if [ -d "$PROJECT_ROOT" ]; then
    cd "$PROJECT_ROOT"
else
    echo "Warning: $PROJECT_ROOT not found, using current directory."
    PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
    cd "$PROJECT_ROOT"
fi
echo "[*] Terminating lingering gradle processes & cleaning build dirs..."
sudo pkill -f gradle 2>/dev/null || true
sudo rm -rf erp-backend/build 2>/dev/null || true
echo "[*] Ensuring correct project file permissions..."
sudo chown -R $(whoami):$(id -gn) "$PROJECT_ROOT" 2>/dev/null || true
echo "[0/4] Clearing previous log files..."
sudo rm -f backend.log backend_run.log 2>/dev/null
echo "[1/4] Building backend BootJar..."
chmod +x gradlew
./gradlew :erp-backend:clean :erp-backend:bootJar --no-daemon
echo "[2/4] Fixing directory permissions & creating data dirs..."
sudo mkdir -p /var/lib/asterisk/sounds/custom /data/smart-erp-crm/mysql /data/smart-erp-crm/storage /data/smart-erp-crm/recordings /data/smart-erp-crm/chatwoot/postgres
sudo chmod -R 777 /var/lib/asterisk/sounds/custom
sudo chmod -R 777 /data/smart-erp-crm
echo "[3/4] Cleaning up existing Docker resources..."
sudo docker compose down
echo "[4/4] Launching Smart ERP CRM services via Docker Compose (Port 3308)..."
sudo docker compose up --build --force-recreate -d
sudo docker compose logs -f backend > backend_run.log 2>&1 &
echo "=================================================="
echo " ✅ SMART-ERP-CRM STARTED SUCCESSFULLY (DB @ 3308)!"
echo " --------------------------------------------------"
echo " - External URL : http://smart-erp.duckdns.org:18889"
echo " - Internal Web : http://172.30.1.11:18889"
echo " - Backend API  : http://172.30.1.11:8090"
echo " - CRM DB Port  : 3308 (Persistent Volume Preserved)"
echo "=================================================="
