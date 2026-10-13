package com.descartes.controller;

import jakarta.validation.Valid;
import com.descartes.model.LocationMedia;
import com.descartes.model.LocationMediaRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.descartes.repository.LocationMediaRepo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/locations/{locationId}/media")
public class LocationMediaController {

    private final LocationMediaRepo locationMediaRepo;

    public LocationMediaController(LocationMediaRepo locationMediaRepo) {
        this.locationMediaRepo = locationMediaRepo;
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
        locationMediaRepo.deleteById(locationId, id);
    }

    private static void apply(LocationMedia item, LocationMediaRequest request) {
        item.setId(request.id());
        item.setUrl(request.url());
        item.setCategory(request.category());
        item.setAttributes(request.attributes());
    }

}