#!/bin/bash
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER order_user WITH PASSWORD '$ORDER_DB_PASSWORD';
    CREATE DATABASE order_db OWNER order_user;
EOSQL