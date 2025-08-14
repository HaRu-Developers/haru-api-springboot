#!/bin/bash

cd /home/ubuntu/cicd/spring

# 네트워크 보장
  docker network inspect haru-network >/dev/null 2>&1 || docker network create haru-network

APP_NAME="haru"

# NGINX 설정 관련
NGINX_CONF_PATH="/etc/nginx"
BLUE_CONF="blue.conf"
GREEN_CONF="green.conf"
DEFAULT_CONF="nginx.conf"
MAX_RETRIES=3

# 활성화된 서비스 확인 및 스위칭 대상 결정
determine_target() {
  if docker compose -f docker-compose.yml ps | grep -q "app-blue.*Up"; then
    TARGET="green"
    OLD="blue"
  elif docker compose -f docker-compose.yml ps | grep -q "app-green.*Up"; then
    TARGET="blue"
    OLD="green"
  else
    TARGET="blue"  # 첫 실행 시 기본값
    OLD="none"
  fi

  echo "TARGET: $TARGET"
  echo "OLD: $OLD"
}
# 헬스체크 실패 시 롤백 처리
health_check() {
  local URL=$1
  local RETRIES=0
  local ORIGINAL_TARGET=$TARGET  # 원래 TARGET 값을 저장

  while [ $RETRIES -lt $MAX_RETRIES ]; do
    echo "Checking service at $URL... (attempt: $((RETRIES + 1)))"
    sleep 3

    # 현재 실행 중인 컨테이너 확인
    CONTAINER_RUNNING=$(docker ps --filter "name=app-$TARGET" --format '{{.Names}}')

    if [ "$CONTAINER_RUNNING" = "app-$TARGET" ]; then
      echo "$TARGET container is running."
      return 0  # 컨테이너가 실행 중이라면 헬스체크 성공
    else
      echo "$TARGET container is not running."
    fi

    RETRIES=$((RETRIES + 1))
  done

  # 헬스체크 실패 시 롤백 처리
  echo "Health check failed after $MAX_RETRIES attempts."
  echo "Rolling back to the original target: $ORIGINAL_TARGET"

  # TARGET을 원래 값으로 롤백
  TARGET=$ORIGINAL_TARGET
  echo "Rolled back TARGET: $TARGET"

  # 로그 파일에 실패 기록
  echo "Failed health check for $TARGET container" > /home/ubuntu/cicd/health_check_failure.log

  # docker-compose down 대신 실패 기록 후 종료
  exit 1
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
    sudo docker stop "app-$OLD"

  fi
}

# 메인 실행 로직
main() {
  determine_target

 # 최신 이미지 pull
  echo "Pulling latest image for app-$TARGET..."
  docker compose -f docker-compose.yml pull "app-$TARGET"

  # 대상 컨테이너 실행
  echo "Starting $TARGET container..."
  docker compose -f docker-compose.yml up -d "app-$TARGET"

  # 헬스체크
  if [ "$TARGET" = "blue" ]; then
    health_check "http://127.0.0.1:8080/actuator/health"
  else
    health_check "http://127.0.0.1:8081/actuator/health"
  fi

  # NGINX 설정 스위칭
  switch_nginx_conf

  # 이전 컨테이너 종료
  down_old_container

  echo "Deployment to $TARGET completed successfully!"

  echo "Cleaning up dangling Docker images..."
  docker image prune -f
}

# 스크립트 실행
main