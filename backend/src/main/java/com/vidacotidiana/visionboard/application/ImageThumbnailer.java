package com.vidacotidiana.visionboard.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * Reduce una imagen a lo que el lienzo llega a enseñar.
 *
 * POR QUÉ EXISTE: un Vision Board con diecinueve fotos descargaba
 * diecinueve originales. En un teléfono, con el tablero encuadrado, ninguna
 * de esas fotos se dibuja a más de unos cuatrocientos píxeles: el resto de
 * los bytes se descargan, se decodifican y se tiran. El tiempo hasta ver el
 * tablero lo marcaba algo que nadie mira.
 *
 * SOLO JPEG Y PNG. WEBP no tiene escritor en el ImageIO del JDK, y un GIF
 * reescrito perdería la animación, que es justo lo que alguien busca al
 * poner un GIF. En esos dos casos devuelve {@code null} y el original se
 * sirve tal cual: la miniatura es una optimización, nunca un requisito.
 *
 * NUNCA LANZA. Una imagen que no se deja escalar —rara, corrupta, con un
 * perfil de color que ImageIO no entiende— no puede impedir que se suba:
 * se registra y se sigue sin miniatura.
 */
@Component
public class ImageThumbnailer {

    private static final Logger log = LoggerFactory.getLogger(ImageThumbnailer.class);

    /**
     * Borde mayor de la miniatura.
     *
     * 800 px cubre con holgura lo que el móvil dibuja con el tablero
     * encuadrado y aguanta acercarse un par de pasos sin que se note. El
     * cliente Android decodifica a este mismo tamaño
     * (`VisionBoardRepository.MAX_DECODE_EDGE`), así que por encima de esto
     * cada píxel de más se descarta al decodificar.
     */
    public static final int MAX_EDGE = 800;

    private static final float JPEG_QUALITY = 0.82f;

    /** La miniatura, o {@code null} si este formato no se reescribe. */
    public Thumbnail shrink(byte[] original, String contentType) {
        boolean png = "image/png".equalsIgnoreCase(contentType);
        boolean jpeg = "image/jpeg".equalsIgnoreCase(contentType);
        if (!png && !jpeg) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                return null;
            }
            int w = source.getWidth();
            int h = source.getHeight();
            int longest = Math.max(w, h);
            if (longest <= MAX_EDGE) {
                // Ya es pequeña: una "miniatura" del mismo tamaño solo
                // duplicaría los bytes guardados sin ahorrar nada al servir.
                return null;
            }
            double scale = (double) MAX_EDGE / longest;
            int tw = Math.max(1, (int) Math.round(w * scale));
            int th = Math.max(1, (int) Math.round(h * scale));

            // PNG conserva su canal alfa (un recorte con fondo quitado deja
            // de serlo si se aplasta sobre blanco); JPEG no lo tiene.
            BufferedImage target = new BufferedImage(tw, th, png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, tw, th, null);
            g.dispose();

            byte[] bytes = png ? writePng(target) : writeJpeg(target);
            if (bytes == null || bytes.length >= original.length) {
                // Si no ahorra, no compensa guardarla.
                return null;
            }
            return new Thumbnail(bytes, contentType);
        } catch (Exception | OutOfMemoryError e) {
            log.warn("No se pudo generar la miniatura ({}): {}", contentType, e.toString());
            return null;
        }
    }

    private byte[] writePng(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return ImageIO.write(image, "png", out) ? out.toByteArray() : null;
    }

    private byte[] writeJpeg(BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            return null;
        }
        ImageWriter writer = writers.next();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(JPEG_QUALITY);
                }
                writer.write(null, new IIOImage(image, null, null), params);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    public record Thumbnail(byte[] data, String contentType) {
    }
}
