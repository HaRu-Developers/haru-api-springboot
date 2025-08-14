#!/bin/bash

# 한 명령어라도 실패하면 스크립트 즉시 종료
set -e

cd /home/ubuntu/cicd/spring

# 환경변수 설정
APP_NAME="haru"
TARGET=""
OLD=""
NGINX_CONF_PATH="/etc/nginx"
BLUE_CONF="blue.conf"
GREEN_CONF="green.conf"
DEFAULT_CONF="nginx.conf"
MAX_RETRIES=3
HEALTH_CHECK_PORT=""

# 네트워크 존재 확인 및 생성
ensure_network() {
  echo "Checking Docker network 'haru-network'..."
  if ! docker network inspect haru-network >/dev/null 2>&1; then
    echo "Network 'haru-network' not found. Creating it..."
    docker network create haru-network
  else
    echo "Network 'haru-network' already exists."
  fi
}

# 활성화된 서비스 확인 및 스위칭 대상 결정
determine_target() {
  if docker compose -f docker-compose.yml ps | grep -q "app-blue.*Up"; then
    TARGET="green"
    OLD="blue"
    HEALTH_CHECK_PORT="8081"
  elif docker compose -f docker-compose.yml ps | grep -q "app-green.*Up"; then
    TARGET="blue"
    OLD="green"
    HEALTH_CHECK_PORT="8080"
  else
    TARGET="blue"  # 첫 실행 시 기본값
    OLD="none"
    HEALTH_CHECK_PORT="8080"
  fi

  echo "TARGET: $TARGET"
  echo "OLD: $OLD"
}

# 헬스체크 실패 시 롤백 처리
health_check() {
  local RETRIES=0
    local URL="http://localhost:$HEALTH_CHECK_PORT/actuator/health"
    echo "Starting health check for the new '$TARGET' container on port $HEALTH_CHECK_PORT..."

    # 새로운 컨테이너가 실행될 때까지 충분히 대기
    sleep 10

    while [ $RETRIES -lt $MAX_RETRIES ]; do
      echo "Attempt $((RETRIES + 1)) of $MAX_RETRIES: Checking $URL..."

      # curl 명령어로 애플리케이션의 헬스 체크 엔드포인트에 HTTP 요청
      STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL" || true)

      if [ "$STATUS_CODE" -eq 200 ]; then
        echo "Health check succeeded! Container '$TARGET' is healthy."
        return 0
      else
        echo "Health check failed with status code: $STATUS_CODE. Retrying in 5 seconds..."
        sleep 5
      fi

      RETRIES=$((RETRIES + 1))
    done

    echo "Health check failed after $MAX_RETRIES attempts."
    return 1 # 헬스 체크 실패 시 1을 반환하여 스크립트 롤백을 유도
}

# NGINX 설정 스위칭 함수
switch_nginx_conf() {
  if [ "$TARGET" = "blue" ]; then
    sudo cp "${NGINX_CONF_PATH}/${BLUE_CONF}" "${NGINX_CONF_PATH}/${DEFAULT_CONF}"
  else
    sudo cp "${NGINX_CONF_PATH}/${GREEN_CONF}" "${NGINX_CONF_PATH}/${DEFAULT_CONF}"
  fi

  echo "Reloading NGINX configuration..."
  nginx -s reload
}

# 이전 컨테이너 종료 함수
down_old_container() {
  if [ "$OLD" != "none" ]; then
    echo "Stopping old container: $OLD"
    docker compose -f docker-compose.yml stop "app-$OLD"
    docker compose -f docker-compose.yml rm -f "app-$OLD"
  fi
}

# 메인 실행 로직
main() {
  ensure_network
  determine_target

  # 컨테이너 충돌 방지를 위해, 동일한 이름의 컨테이너가 있을 경우 미리 삭제
  echo "Removing any existing container with the name 'app-$TARGET'..."
  docker rm -f "app-$TARGET" 2>/dev/null || true

  # 최신 이미지 풀
  echo "Pulling the latest image for '$TARGET' service..."
  docker compose -f docker-compose.yml pull "app-$TARGET"

  # 대상 컨테이너 실행
  echo "Starting '$TARGET' container..."
  docker compose -f docker-compose.yml up -d "app-$TARGET"


  # 헬스 체크 및 롤백 로직
    if ! health_check; then
      echo "Health check failed. Initiating rollback..."

      # 실패한 컨테이너를 중지하고 제거
      echo "Removing failed container: 'app-$TARGET'..."
      docker compose -f docker-compose.yml down --remove-orphans "app-$TARGET" || true

      echo "Rollback complete. The previous version remains active."
      exit 1 # 스크립트 종료
    fi

    # 헬스 체크 성공 시, NGINX 설정 스위칭
    switch_nginx_conf

    # 이전 컨테이너 종료
    down_old_container

    echo "Deployment to '$TARGET' completed successfully!"
    echo "Cleaning up dangling Docker images..."
    docker image prune -f

    echo "Deployment finished at $(date)"
}

# 스크립트 실행
main