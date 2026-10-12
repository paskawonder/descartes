package com.descartes.controller;

import com.descartes.model.LocationRequest;
import com.descartes.model.Location;
import com.descartes.repository.LocationRepo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationRepo locationRepo;

    public LocationController(LocationRepo locationRepo) {
        this.locationRepo = locationRepo;
    }

    @PostMapping
    public Location create(@Valid @RequestBody LocationRequest request) {
        Location location = new Location();
        location.setId(request.id().toString());
        apply(location, request);
        locationRepo.save(location);
        return location;
    }

    @GetMapping("/{id}")
    public Location get(@PathVariable("id") String id) {
        return find(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        locationRepo.delete(id);
    }

    @GetMapping
    public List<Location> list() {
        return locationRepo.findAll(1000);
    }

    private Location find(String id) {
        return locationRepo.findById(id).orElseThrow();
    }

    private static void apply(Location location, LocationRequest request) {
        location.setName(request.name());
        location.setCountry(request.country());
        location.setLat(request.lat());
        location.setLon(request.lon());
        location.setMapsLink(request.mapsLink());
        location.setAttributes(request.attributes());
    }

}
