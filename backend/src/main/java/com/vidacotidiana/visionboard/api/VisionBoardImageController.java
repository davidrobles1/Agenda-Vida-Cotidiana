package com.vidacotidiana.visionboard.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.visionboard.api.dto.VisionBoardImageResponse;
import com.vidacotidiana.visionboard.application.VisionBoardImageService;
import com.vidacotidiana.visionboard.domain.VisionBoardImage;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * BLOQUE B (post-MVP): upload + read-back for Vision Board images — see
 * VisionBoardImageService's own doc comment for the storage decision.
 * Deliberately its own controller/path (not nested under
 * {@code /vision-boards/{id}}) — an uploaded image isn't owned by one
 * specific board (see V10 migration's doc comment), and this keeps
 * VisionBoardController focused on the board/element resources it already
 * owns, same "one controller per aggregate root" shape as the rest of this
 * API.
 *
 * Both endpoints require the normal bearer JWT (SecurityConfig's
 * `anyRequest().authenticated()` already covers this path, nothing new to
 * configure) — the GET is deliberately *not* public, so the frontend
 * resolves `<img>` sources via an authenticated fetch + object URL rather
 * than a plain `<img src="...">` (see VisionBoardCanvas's
 * `useVisionBoardImageSrc` doc comment on the client side).
 */
@RestController
@RequestMapping("/api/v1/vision-board-images")
public class VisionBoardImageController {

    private final VisionBoardImageService imageService;
    private final CurrentUser currentUser;

    public VisionBoardImageController(VisionBoardImageService imageService, CurrentUser currentUser) {
        this.imageService = imageService;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VisionBoardImageResponse> upload(@RequestParam("file") MultipartFile file) {
        VisionBoardImage image = imageService.upload(currentUser.userId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(VisionBoardImageResponse.from(image));
    }

    /**
     * Los bytes de una imagen.
     *
     * {@code variant=thumb} pide la rendición reducida (borde mayor 800 px,
     * ver {@code ImageThumbnailer}) en vez del original. Es lo que usa un
     * lienzo para pintar: descargar una foto de varios MB para dibujarla del
     * tamaño de un sello era lo que hacía que abrir un tablero con veinte
     * imágenes tardara. Si esa imagen no tiene miniatura —formato que no se
     * reescribe, o ya era pequeña— se sirve el original, así que el cliente
     * puede pedir siempre la miniatura sin preguntar antes.
     *
     * El {@code ETag} permite que un cliente que ya la tiene reciba un 304 de
     * unos pocos bytes en vez de la imagen entera. Lleva la variante dentro:
     * el original y su miniatura son cuerpos distintos bajo la misma URL.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(
            @PathVariable UUID id,
            @RequestParam(name = "variant", required = false) String variant,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        VisionBoardImage image = imageService.getOwnedOrThrow(id, currentUser.userId());
        boolean wantsThumb = "thumb".equalsIgnoreCase(variant);
        if (wantsThumb && !image.hasThumb()) {
            // Las filas anteriores a la V39 no traen miniatura: se genera en
            // esta petición y se guarda, así que solo la primera la paga.
            image = imageService.ensureThumb(image);
        }
        boolean serveThumb = wantsThumb && image.hasThumb();

        String etag = "\"" + id + (serveThumb ? "-t" : "-o") + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate())
                    .build();
        }

        byte[] body = serveThumb ? image.getThumbData() : image.getData();
        String contentType = serveThumb ? image.getThumbContentType() : image.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // Immutable content (a new upload always gets a new id, never
                // overwrites this one) — safe to cache aggressively per-user.
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate())
                .eTag(etag)
                .body(body);
    }
}
