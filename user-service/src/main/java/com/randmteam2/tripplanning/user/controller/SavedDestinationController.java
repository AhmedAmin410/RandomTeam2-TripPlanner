package com.randmteam2.tripplanning.user.controller;

import com.randmteam2.tripplanning.user.model.SavedDestination;
import com.randmteam2.tripplanning.user.service.SavedDestinationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class SavedDestinationController {

    private final SavedDestinationService service;

    public SavedDestinationController(SavedDestinationService service) {
        this.service = service;
    }

    @PostMapping
    public SavedDestination create(@RequestBody SavedDestination destination) {
        return service.create(destination);
    }

    @GetMapping
    public List<SavedDestination> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SavedDestination getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public SavedDestination update(@PathVariable Long id,
                                   @RequestBody SavedDestination destination) {
        return service.update(id, destination);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}