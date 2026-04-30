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
    private final UserCacheInvalidationService cacheInvalidationService;

    public SavedDestinationService(SavedDestinationRepository repository,
                                   UserCacheInvalidationService cacheInvalidationService) {
        this.repository = repository;
        this.cacheInvalidationService = cacheInvalidationService;
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

        if (updated.getDefault() != null && updated.getDefault()) {

            List<SavedDestination> userDestinations =
                    repository.findByUser_Id(existing.getUser().getId());

            for (SavedDestination d : userDestinations) {
                d.setDefault(false);
            }

            existing.setDefault(true);

        } else if (updated.getDefault() != null) {
            existing.setDefault(false);
        }

        if (updated.getMetadata() != null)
            existing.setMetadata(updated.getMetadata());

        SavedDestination saved = repository.save(existing);
        cacheInvalidationService.evictUserReadCaches();
        return saved;
    }

    public void delete(Long id) {

        SavedDestination dest = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saved destination not found"
                ));

        repository.delete(dest);
        cacheInvalidationService.evictUserReadCaches();
    }
    public List<SavedDestination> getByUserId(Long userId) {
        return repository.findByUser_Id(userId);
    }


}