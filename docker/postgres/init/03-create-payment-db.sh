#!/bin/bash
# Runs once, when the Postgres data volume is created for the first time
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER payment_user WITH PASSWORD '$PAYMENT_DB_PASSWORD';
    CREATE DATABASE payment_db OWNER payment_user;
EOSQL