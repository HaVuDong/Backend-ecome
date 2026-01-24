-- Add main_image_public_id to products for storing Cloudinary public_id
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS main_image_public_id VARCHAR(255);
