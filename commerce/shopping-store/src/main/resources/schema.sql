
CREATE SCHEMA IF NOT EXISTS store;


CREATE TABLE IF NOT EXISTS store.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_src VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    quantity_state VARCHAR(20) NOT NULL,
    product_state VARCHAR(20) NOT NULL,
    product_category VARCHAR(20) NOT NULL
);