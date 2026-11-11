#!/bin/bash
# Runs once, when the Postgres data volume is created for the first time
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER product_user WITH PASSWORD '$PRODUCT_DB_PASSWORD';
    CREATE DATABASE product_db OWNER product_user;
EOSQL