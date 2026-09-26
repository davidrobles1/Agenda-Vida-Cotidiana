-- V39__vision_board_image_thumbnail.sql
-- Una miniatura junto al original, para que el lienzo no tenga que
-- descargar la foto entera solo para pintarla del tamaño de un sello.
--
-- EL PROBLEMA MEDIDO: abrir un Vision Board con diecinueve imágenes
-- descargaba diecinueve originales —cientos de KB cada uno, hasta 23 MB por
-- el límite de subida— cuando en un teléfono ninguna de esas fotos se
-- dibuja a más de unos cientos de píxeles. El tiempo hasta ver el tablero
-- lo marcaba el tamaño de unos bytes que nadie llega a mirar.
--
-- POR QUÉ AL SUBIR Y NO AL SERVIR: redimensionar en cada GET gasta CPU una
-- y otra vez por la misma imagen. Subir ocurre una vez; servir, muchas. Se
-- paga el escalado donde se paga una sola vez.
--
-- NULLABLE A PROPÓSITO: las filas que ya existen no tienen miniatura, y un
-- formato que el servidor no sabe reescribir (WEBP, o un GIF animado, que
-- perdería la animación) tampoco la tendrá nunca. En los dos casos se
-- sirve el original, que es exactamente el comportamiento de hoy. La
-- miniatura es una optimización, no un requisito: nada depende de que
-- exista.

ALTER TABLE vision_board_images
    ADD COLUMN thumb_data         BYTEA,
    ADD COLUMN thumb_content_type VARCHAR(64),
    ADD COLUMN thumb_size_bytes   BIGINT;

COMMENT ON COLUMN vision_board_images.thumb_data IS
    'Rendición reducida (borde mayor 800 px) para el lienzo. NULL = sírvase el original.';
