package com.descartes.controller;

import com.descartes.config.S3Config.S3Settings;
import com.descartes.repository.LocationRepo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.UUID;

/** Issues short-lived presigned S3 PUT URLs; the browser uploads the file straight to S3. */
@RestController
@RequestMapping("/api/locations/{locationId}/media")
public class MediaUploadController {

    public record UploadUrlRequest(@NotBlank String fileName, @NotBlank String contentType) {}
    public record UploadUrlResponse(String mediaId, String uploadUrl, String publicUrl) {}

    private final S3Presigner presigner;
    private final S3Settings s3;

    public MediaUploadController(S3Presigner presigner, S3Settings s3) {
        this.presigner = presigner;
        this.s3 = s3;
    }

    @PostMapping("/upload-url")
    public UploadUrlResponse uploadUrl(@PathVariable("locationId") String locationId,
                                       @Valid @RequestBody UploadUrlRequest req) {
        if (!req.contentType().matches("^(image|video)/[\\w.+-]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image and video uploads are allowed");
        }
        String safeName = req.fileName().replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.length() > 100) safeName = safeName.substring(safeName.length() - 100);

        String mediaId = UUID.randomUUID().toString();
        String key = "locations/%s/%s/%s".formatted(locationId, mediaId, safeName);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(s3.bucket()).key(key).contentType(req.contentType()).build();
        String uploadUrl = presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(10)).putObjectRequest(put)).url().toString();
        String publicUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(s3.bucket(), s3.region(), key);
        return new UploadUrlResponse(mediaId, uploadUrl, publicUrl);
    }
}