package com.descartes.controller;

import com.descartes.config.S3Config.S3Settings;
import jakarta.validation.Valid;
import com.descartes.model.LocationMedia;
import com.descartes.model.LocationMediaRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.descartes.repository.LocationMediaRepo;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/locations/{locationId}/media")
public class LocationMediaController {

    private final LocationMediaRepo locationMediaRepo;
    private final S3Client s3Client;
    private final S3Settings s3Settings;

    public LocationMediaController(LocationMediaRepo locationMediaRepo, S3Client s3Client, S3Settings s3Settings) {
        this.locationMediaRepo = locationMediaRepo;
        this.s3Client = s3Client;
        this.s3Settings = s3Settings;
    }

    @PostMapping
    public LocationMedia create(@PathVariable("locationId") String locationId,
                                @Valid @RequestBody LocationMediaRequest request) {
        Optional<LocationMedia> existing = locationMediaRepo.findById(locationId, request.id());
        LocationMedia item = existing.orElseGet(LocationMedia::new);
        apply(item, request);
        item.setLocationId(locationId);
        locationMediaRepo.save(item);
        return item;
    }

    @GetMapping
    public List<LocationMedia> list(@PathVariable("locationId") String locationId,
                                    @RequestParam(value = "category", required = false) String category) {
        List<LocationMedia> medias = locationMediaRepo.findByLocationId(locationId);
        if (StringUtils.isBlank(category)) {
            return medias;
        }
        return medias.stream().filter(e -> e.getCategory().toLowerCase().equals(category)).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("locationId") String locationId,
                       @PathVariable("id") String id) {
        LocationMedia media = locationMediaRepo.findById(locationId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        String key = s3ObjectKey(media, locationId, id);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3Settings.bucket()).key(key).build());
        locationMediaRepo.deleteById(locationId, id);
    }

    private String s3ObjectKey(LocationMedia media, String locationId, String id) {
        if (media.getUrl() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Media URL is missing");
        }
        URI uri;
        try {
            uri = URI.create(media.getUrl());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Media URL is invalid", e);
        }
        String expectedHost = "%s.s3.%s.amazonaws.com".formatted(s3Settings.bucket(), s3Settings.region());
        String path = uri.getPath();
        String prefix = "locations/%s/%s/".formatted(locationId, id);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !expectedHost.equalsIgnoreCase(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || path == null || !path.startsWith("/" + prefix) || path.length() <= prefix.length() + 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Media URL does not identify its S3 object");
        }
        return path.substring(1);
    }

    private static void apply(LocationMedia item, LocationMediaRequest request) {
        item.setId(request.id());
        item.setUrl(request.url());
        item.setCategory(request.category());
        item.setAttributes(request.attributes());
    }

}