
docker run --name postgres-analyzer -e POSTGRES_DB=analyzer_db -e POSTGRES_USER=analyzer_user -e POSTGRES_PASSWORD=analyzer_password -p 5432:5432 -d postgres:16.1


docker run --name postgres-commerce -e  POSTGRES_DB=commerce_db -e POSTGRES_USER=commerce_user -e POSTGRES_PASSWORD=commerce_password -p 5433:5432 -d postgres:16.1