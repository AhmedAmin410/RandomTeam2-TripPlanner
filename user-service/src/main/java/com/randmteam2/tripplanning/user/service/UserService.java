package com.randmteam2.tripplanning.user.service;

import com.randmteam2.tripplanning.user.adapter.ObjectArrayDtoAdapter;
import com.randmteam2.tripplanning.user.dto.SavedDestinationDTO;
import com.randmteam2.tripplanning.user.dto.TopTravelerDTO;
import com.randmteam2.tripplanning.user.dto.TravelStyleUserDTO;
import com.randmteam2.tripplanning.user.dto.UserBookingTotalDTO;
import com.randmteam2.tripplanning.user.dto.UserProfileDTO;
import com.randmteam2.tripplanning.user.dto.UserTripSummaryAggregateDTO;
import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import com.randmteam2.tripplanning.user.feign.BookingServiceClient;
import com.randmteam2.tripplanning.user.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.user.model.Role;
import com.randmteam2.tripplanning.user.model.SavedDestination;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.model.UserStatus;
import com.randmteam2.tripplanning.user.observer.UserEventPublisher;
import com.randmteam2.tripplanning.user.repository.AuthEventRepository;
import com.randmteam2.tripplanning.user.repository.SavedDestinationRepository;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final SavedDestinationRepository savedDestinationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCacheInvalidationService cacheInvalidationService;
    private final AuthEventRepository authEventRepository;
    private final UserEventPublisher eventPublisher;
    private final ItineraryServiceClient itineraryServiceClient;
    private final BookingServiceClient bookingServiceClient;
    private final ObjectArrayDtoAdapter objectArrayDtoAdapter;
    private final RabbitTemplate rabbitTemplate;

    public UserService(UserRepository userRepository,
                       SavedDestinationRepository savedDestinationRepository,
                       PasswordEncoder passwordEncoder,
                       UserCacheInvalidationService cacheInvalidationService,
                       AuthEventRepository authEventRepository,
                       UserEventPublisher eventPublisher,
                       ItineraryServiceClient itineraryServiceClient,
                       BookingServiceClient bookingServiceClient,
                       ObjectArrayDtoAdapter objectArrayDtoAdapter,
                       RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.savedDestinationRepository = savedDestinationRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheInvalidationService = cacheInvalidationService;
        this.authEventRepository = authEventRepository;
        this.eventPublisher = eventPublisher;
        this.itineraryServiceClient = itineraryServiceClient;
        this.bookingServiceClient = bookingServiceClient;
        this.objectArrayDtoAdapter = objectArrayDtoAdapter;
        this.rabbitTemplate = rabbitTemplate;
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.TRAVELER);
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        User saved = userRepository.save(user);
        eventPublisher.notifyObservers("USER_CREATED", Map.of("userId", saved.getId(), "email", saved.getEmail()));
        cacheInvalidationService.evictUserReadCaches();
        try {
            rabbitTemplate.convertAndSend("user.events", "user.registered",
                    Map.of(
                            "eventType", "user.registered",
                            "userId", saved.getId(),
                            "email", saved.getEmail(),
                            "role", saved.getRole().name()
                    ));
            log.info("Published user.registered for userId={}", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to publish user.registered for userId={}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Cacheable(value = "user-service", key = "'user::' + #id")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateUser(Long id, User updatedUser) {
        cacheInvalidationService.evictUserReadCaches();
        User existingUser = getUserById(id);

        if (updatedUser.getStatus() != null &&
                updatedUser.getStatus() == UserStatus.DEACTIVATED &&
                updatedUser.getName() == null &&
                updatedUser.getEmail() == null &&
                updatedUser.getPassword() == null &&
                updatedUser.getPhone() == null &&
                updatedUser.getRole() == null &&
                updatedUser.getPreferences() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active itinerary");
        }

        if (updatedUser.getName() != null) existingUser.setName(updatedUser.getName());
        if (updatedUser.getEmail() != null) existingUser.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null)
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        if (updatedUser.getPhone() != null) existingUser.setPhone(updatedUser.getPhone());
        if (updatedUser.getRole() != null) existingUser.setRole(updatedUser.getRole());
        if (updatedUser.getPreferences() != null) existingUser.setPreferences(updatedUser.getPreferences());
        if (updatedUser.getStatus() != null) existingUser.setStatus(updatedUser.getStatus());

        User saved = userRepository.save(existingUser);
        eventPublisher.notifyObservers("USER_UPDATED", Map.of("userId", saved.getId()));
        return saved;
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
        eventPublisher.notifyObservers("USER_DELETED", Map.of("userId", id));
        cacheInvalidationService.evictUserReadCaches();
    }

    public User updatePreferences(Long id, Map<String, Object> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String, Object> current = user.getPreferences();
        if (current == null) current = new HashMap<>();
        current.putAll(updates);
        user.setPreferences(current);
        User saved = userRepository.save(user);
        eventPublisher.notifyObservers("USER_UPDATED", Map.of("userId", saved.getId()));
        cacheInvalidationService.evictUserReadCaches();
        return saved;
    }

    // S1-F3: replaced cross-service SQL with Feign → itinerary-service (Builder + Adapter preserved)
    @Cacheable(value = "user-service", key = "'S1-F3::' + #userId")
    public UserTripSummaryDTO getUserTripSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserTripSummaryAggregateDTO aggregate;
        try {
            log.info("Calling itineraryServiceClient.getUserItinerarySummary with args={}", userId);
            aggregate = itineraryServiceClient.getUserItinerarySummary(userId);
            log.info("itineraryServiceClient.getUserItinerarySummary returned successfully");
        } catch (FeignException.NotFound e) {
            aggregate = UserTripSummaryAggregateDTO.empty();
        } catch (FeignException e) {
            log.warn("Feign call to itinerary-service unavailable (userId={}): {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Itinerary service temporarily unavailable");
        }

        return objectArrayDtoAdapter.adapt(aggregate, user.getId(), user.getName());
    }

    @Cacheable(value = "user-service", key = "'S1-F5::' + #key + '::' + #value")
    public List<User> searchByPreference(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must not be empty");
        }
        return userRepository.findByPreference(key, value);
    }

    @Cacheable(value = "user-service",
            key = "'S1-F1::' + (#name == null ? '' : #name) + '::' + (#role == null ? '' : #role.name()) + '::' + (#email == null ? '' : #email)")
    public List<User> searchUsers(String name, Role role, String email) {
        if (name != null && name.trim().isEmpty()) name = null;
        if (email != null && email.trim().isEmpty()) email = null;
        return userRepository.searchUsers(name, role != null ? role.name() : null, email);
    }

    public SavedDestination createSavedDestination(Long userId, SavedDestination destination) {
        User user = getUserById(userId);
        destination.setUser(user);
        SavedDestination saved = savedDestinationRepository.save(destination);
        cacheInvalidationService.evictUserReadCaches();
        return saved;
    }

    public List<SavedDestination> getSavedDestinations(Long userId) {
        getUserById(userId);
        return savedDestinationRepository.findByUser_Id(userId);
    }

    @Cacheable(value = "user-service", key = "'saved-destination::' + #id")
    public SavedDestination getSavedDestinationById(Long userId, Long id) {
        getUserById(userId);
        return savedDestinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SavedDestination not found"));
    }

    // S1-F4: replaced cross-service SQL countActiveItineraries with Feign → itinerary-service
    public User deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        int activeCount;
        try {
            log.info("Calling itineraryServiceClient.getActiveItineraryCount with args={}", id);
            activeCount = itineraryServiceClient.getActiveItineraryCount(id);
            log.info("itineraryServiceClient.getActiveItineraryCount returned successfully");
        } catch (FeignException e) {
            log.warn("Feign call to itinerary-service failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Itinerary service temporarily unavailable");
        }

        if (activeCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active itineraries");
        }

        user.setStatus(UserStatus.DEACTIVATED);
        User saved = userRepository.save(user);
        eventPublisher.notifyObservers("USER_DEACTIVATED", Map.of("userId", saved.getId()));
        cacheInvalidationService.evictUserReadCaches();

        // S1-READ-DB: publish user.deactivated to user.events exchange (S1-EVENTS wires the topology)
        try {
            rabbitTemplate.convertAndSend("user.events", "user.deactivated",
                    Map.of(
                            "eventType", "user.deactivated",
                            "userId", saved.getId()
                    ));
            log.info("Published user.deactivated for userId={}", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to publish user.deactivated for userId={}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    // S1-F6: replaced cross-service SQL JOIN with Feign → booking-service per user
    @Cacheable(value = "user-service", key = "'S1-F6::' + #startDate + '::' + #endDate + '::' + #limit")
    public List<TopTravelerDTO> getTopTravelers(String startDate, String endDate, Integer limit) {
        if (startDate.compareTo(endDate) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }

        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> {
                    UserBookingTotalDTO total;
                    try {
                        log.info("Calling bookingServiceClient.getUserBookingTotal with args={}", user.getId());
                        total = bookingServiceClient.getUserBookingTotal(user.getId(), startDate, endDate);
                        log.info("bookingServiceClient.getUserBookingTotal returned successfully");
                    } catch (FeignException e) {
                        log.warn("Feign call to booking-service failed for user {}: {}", user.getId(), e.getMessage());
                        total = new UserBookingTotalDTO();
                        total.setUserId(user.getId());
                        total.setTotalAmount(0.0);
                        total.setTripCount(0L);
                    }
                    return TopTravelerDTO.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .totalSpent(total.getTotalAmount() != null ? total.getTotalAmount() : 0.0)
                            .tripCount(total.getTripCount() != null ? total.getTripCount() : 0L)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TopTravelerDTO::totalSpent).reversed())
                .limit(limit)
                .toList();
    }

    public User setDefaultDestination(Long userId, Long destinationId) {
        cacheInvalidationService.evictUserReadCaches();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        SavedDestination target = savedDestinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        if (!target.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination does not belong to user");
        }

        List<SavedDestination> userDestinations = savedDestinationRepository.findByUser_Id(userId);
        for (SavedDestination d : userDestinations) d.setDefault(false);
        target.setDefault(true);
        savedDestinationRepository.saveAll(userDestinations);
        eventPublisher.notifyObservers("DEFAULT_DESTINATION_SET",
                Map.of("userId", userId, "destinationId", destinationId));

        return userRepository.findById(userId).get();
    }

    @Cacheable(value = "user-service", key = "'S1-F8::' + #id")
    public UserProfileDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<SavedDestination> destinations = user.getSavedDestinations();
        List<SavedDestinationDTO> destinationDTOs = destinations.stream()
                .map(d -> new SavedDestinationDTO(
                        d.getLabel(), d.getDestinationName(), d.getCountry(),
                        d.getLatitude(), d.getLongitude(), d.getDefault(), d.getMetadata()))
                .toList();

        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(user.getPreferences())
                .savedDestinations(destinationDTOs)
                .totalSavedDestinations(destinationDTOs.size())
                .build();
    }

    // S1-F9: replaced cross-service SQL JOIN with Feign → itinerary-service per matching user
    @Cacheable(value = "user-service", key = "'S1-F9::' + #style + '::' + #minTrips")
    public List<TravelStyleUserDTO> findUsersByTravelStyle(String style, int minTrips) {
        if (style == null || style.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Style must not be blank");
        }

        List<User> candidates = userRepository.findByTravelStyle(style);

        return candidates.stream()
                .filter(user -> {
                    try {
                        log.info("Calling itineraryServiceClient.getCompletedItineraryCount with args={}", user.getId());
                        long count = itineraryServiceClient.getCompletedItineraryCount(user.getId());
                        log.info("itineraryServiceClient.getCompletedItineraryCount returned successfully");
                        return count >= minTrips;
                    } catch (FeignException e) {
                        log.warn("Feign call to itinerary-service failed for user {}: {}", user.getId(), e.getMessage());
                        return false;
                    }
                })
                .map(u -> TravelStyleUserDTO.builder()
                        .userId(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole().name())
                        .preferences(u.getPreferences())
                        .build())
                .toList();
    }

    @Cacheable(value = "user-service", key = "'S1-F12::' + #userId + '::' + #page + '::' + #size")
    public Map<String, Object> getActivityFeed(Long userId, int page, int size) {
        getUserById(userId);
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        Page<com.randmteam2.tripplanning.user.model.AuthEvent> result =
                authEventRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(page, size));
        List<Map<String, Object>> content = result.getContent().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("action", e.getAction());
                    item.put("timestamp", e.getTimestamp());
                    item.put("details", e.getDetails() != null ? e.getDetails() : Map.of());
                    return item;
                })
                .toList();
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", result.getTotalElements());
        return response;
    }

    public User changeRole(Long id, Role role) {
        User user = getUserById(id);
        String previousRole = user.getRole().name();
        user.setRole(role);
        User saved = userRepository.save(user);
        eventPublisher.notifyObservers("ROLE_CHANGED",
                Map.of("userId", saved.getId(), "previousRole", previousRole, "newRole", role.name()));
        cacheInvalidationService.evictUserReadCaches();
        return saved;
    }

    public User seedAdminUser(String name, String email, String rawPassword, String phone) {
        return userRepository.findByEmail(email)
                .map(existing -> ensureAdminSeed(existing, name, rawPassword, phone))
                .orElseGet(() -> createAdminSeed(name, email, rawPassword, phone));
    }

    private User ensureAdminSeed(User existing, String name, String rawPassword, String phone) {
        existing.setName(name);
        existing.setPhone(phone);
        existing.setRole(Role.ADMIN);
        existing.setStatus(UserStatus.ACTIVE);
        if (!passwordEncoder.matches(rawPassword, existing.getPassword())) {
            existing.setPassword(passwordEncoder.encode(rawPassword));
        }
        return userRepository.save(existing);
    }

    private User createAdminSeed(String name, String email, String rawPassword, String phone) {
        User admin = new User();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setPhone(phone);
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        return userRepository.save(admin);
    }
}
