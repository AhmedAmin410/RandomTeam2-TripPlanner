package com.randmteam2.tripplanning.destination.controller;

import com.randmteam2.tripplanning.destination.dto.DestinationStatusRequest;
import com.randmteam2.tripplanning.destination.model.Destination;
import com.randmteam2.tripplanning.destination.service.DestinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Destination> updateStatus(
            @PathVariable Long id,
            @RequestBody DestinationStatusRequest body) {
        return ResponseEntity.ok(destinationService.updateStatus(id, body.getStatus()));
    }

    @GetMapping("/details/search")
    public ResponseEntity<List<Destination>> searchByDetails(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(destinationService.searchByDetailsKeyValue(key, value, status));
    }
}
