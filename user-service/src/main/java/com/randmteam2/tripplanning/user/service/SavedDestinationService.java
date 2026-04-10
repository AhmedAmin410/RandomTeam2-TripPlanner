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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saved destination not found"
                ));
    }

    public SavedDestination update(Long id, SavedDestination updated) {

        SavedDestination existing = getById(id);

        if (updated.getLabel() != null)
            existing.setLabel(updated.getLabel());

        if (updated.getDestinationName() != null)
            existing.setDestinationName(updated.getDestinationName());

        if (updated.getCountry() != null)
            existing.setCountry(updated.getCountry());

        if (updated.getLatitude() != null)
            existing.setLatitude(updated.getLatitude());

        if (updated.getLongitude() != null)
            existing.setLongitude(updated.getLongitude());

        if (updated.getDefault() != null)
            existing.setDefault(updated.getDefault());

        if (updated.getMetadata() != null)
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