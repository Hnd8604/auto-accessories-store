-- Cập nhật các bản ghi cũ đang bị null thành 0
UPDATE product SET stock_quantity = 0 WHERE stock_quantity IS NULL;

-- Thêm constraint NOT NULL và default value cho cột
ALTER TABLE product ALTER COLUMN stock_quantity SET DEFAULT 0;
ALTER TABLE product ALTER COLUMN stock_quantity SET NOT NULL;
