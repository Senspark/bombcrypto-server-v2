```bash
# Create database
createdb -h localhost -U postgres backend && psql -h localhost -U postgres -d backend -f db/schema.sql

# Build docker
docker compose -f compose.yaml up -d
```