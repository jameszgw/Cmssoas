#!/usr/bin/env bash
# 由权威 Flyway 迁移自动合并生成 deploy/sql 下的表结构脚本(防止与源漂移)。
# 用法:在仓库根目录执行  bash deploy/sql/generate-schema.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PG_DIR="$ROOT/server/license-platform/src/main/resources/db/migration"
MY_DIR="$ROOT/server/license-platform/src/main/resources/db/mysql"
OUT="$ROOT/deploy/sql"

gen() {  # $1=源目录 $2=输出文件 $3=方言名
  local dir="$1" out="$2" name="$3"
  {
    echo "-- ============================================================"
    echo "-- CMSSOAS 全量表结构 + 系统初始化数据($name)"
    echo "-- 自动生成,请勿手改;由 deploy/sql/generate-schema.sh 合并 Flyway 迁移而来。"
    echo "-- 生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "-- 含:全部业务表 DDL + 系统数据(权限/角色/套餐等种子)。"
    echo "-- 注:初始超管账号 admin/8888 由应用首启自动创建(或见 init-admin.sql)。"
    echo "-- ============================================================"
    echo ""
    for f in $(ls "$dir"/V*.sql | sort -V); do
      echo "-- ---------- $(basename "$f") ----------"
      cat "$f"
      echo ""
    done
  } > "$out"
  echo "generated $out"
}

gen "$PG_DIR" "$OUT/schema-postgresql.sql" "PostgreSQL / H2"
gen "$MY_DIR" "$OUT/schema-mysql.sql"      "MySQL 5.7 / 8.0"
