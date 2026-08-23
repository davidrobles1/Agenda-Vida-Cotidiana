-- V14__warranty_documents.sql
-- Pedido explícito del usuario (2026-08-22): "al registrar una garantía
-- subir el archivo de la garantía en formato imagen o pdf." La tabla
-- `warranties` (V5__warranties_maintenance.sql) no tenía ningún campo de
-- archivo — mismo patrón BYTEA que V10 (vision_board_images) y V12
-- (documents), aplicado aquí en vez de crear una tabla aparte: el archivo
-- es 1:1 con la garantía (no se reutiliza entre registros, a diferencia de
-- una imagen de Vision Board), así que vive como columnas nullable en la
-- misma fila en lugar de una tabla relacionada — más simple, sin join
-- adicional para el caso común "traer la garantía completa."
--
-- Nullable a propósito: filas ya existentes (creadas antes de este cambio,
-- si las hay) no tienen archivo — la validación de "obligatorio subir un
-- archivo al registrar" vive en la capa de aplicación (WarrantyService),
-- no como NOT NULL aquí, para no romper datos históricos.

ALTER TABLE warranties
    ADD COLUMN document_content_type VARCHAR(64),
    ADD COLUMN document_size_bytes BIGINT,
    ADD COLUMN document_data BYTEA;
