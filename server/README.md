```bash
# Init gradle
gradle wrapper

# Create database named `bombcrypto`
# Assume that you already have a PostgreSQL instance at your local machine, port 5432, username postgres
createdb -h localhost -U postgres bombcrypto && \
psql -h localhost -U postgres -d bombcrypto -f db/schema.sql \
-f db/init.sql \
-f db/pvp_season_1.sql \
-f db/first_user_add_data.sql

# Deploy docker image & run container
# if on MacOS machine, use the arm64 image
docker build -f deploy/Dockerfile.arm64 -t smartfox deploy/
docker compose -f deploy/compose.yaml up -d

# Copy all jar files into smartfox instance directory
bash run.sh
```