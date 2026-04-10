package com.randmteam2.tripplanning.user.service;

import com.randmteam2.tripplanning.user.model.SavedDestination;
import com.randmteam2.tripplanning.user.repository.SavedDestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class SavedDestinationService {

    private final SavedDestinationRepository repository;

    public SavedDestinationService(SavedDestinationRepository repository) {
        this.repository = repository;
    }

    public SavedDestination create(SavedDestination destination) {
        return repository.save(destination);
    }

    public List<SavedDestination> getAll() {
        return repository.findAll();
    }

    public SavedDestination getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SavedDestination not found"));
    }

    public SavedDestination update(Long id, SavedDestination updated) {
        SavedDestination existing = getById(id);

        existing.setLabel(updated.getLabel());
        existing.setDestinationName(updated.getDestinationName());
        existing.setCountry(updated.getCountry());
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());
        existing.setDefault(updated.getDefault());
        existing.setMetadata(updated.getMetadata());

        return repository.save(existing);
    }

    public void delete(Long id) {

        SavedDestination dest = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saved destination not found"
                ));

        repository.delete(dest);
    }
    public List<SavedDestination> getByUserId(Long userId) {
        return repository.findByUser_Id(userId);
    }
}