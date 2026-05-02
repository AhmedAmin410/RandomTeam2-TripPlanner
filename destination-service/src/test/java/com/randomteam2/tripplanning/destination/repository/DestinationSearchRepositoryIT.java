package com.randomteam2.tripplanning.destination.repository;

import com.randomteam2.tripplanning.destination.model.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DestinationSearchRepositoryIT {

    @Autowired
    private DestinationRepository destinationRepository;

    @BeforeEach
    void clear() {
        destinationRepository.deleteAll();
    }

    @Test
    void specification_categoryOnly() {
        persist("A", Destination.Category.BEACH, 1.0);
        persist("B", Destination.Category.CITY, 5.0);

        List<Destination> out = destinationRepository.findAll(
                DestinationSpecifications.withOptionalFilters(Destination.Category.BEACH, null, null));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getName()).isEqualTo("A");
    }

    @Test
    void specification_minRatingOnly() {
        persist("L", Destination.Category.BEACH, 2.0);
        persist("H", Destination.Category.BEACH, 4.5);

        List<Destination> out = destinationRepository.findAll(
                DestinationSpecifications.withOptionalFilters(null, 4.0, null));

        assertThat(out).extracting(Destination::getName).containsExactlyInAnyOrder("H");
    }

    @Test
    void specification_maxRatingOnly() {
        persist("L", Destination.Category.BEACH, 2.0);
        persist("H", Destination.Category.BEACH, 4.5);

        List<Destination> out = destinationRepository.findAll(
                DestinationSpecifications.withOptionalFilters(null, null, 3.0));

        assertThat(out).extracting(Destination::getName).containsExactlyInAnyOrder("L");
    }

    @Test
    void specification_combined() {
        persist("ok", Destination.Category.MOUNTAIN, 4.2);
        persist("low", Destination.Category.MOUNTAIN, 3.0);
        persist("other", Destination.Category.BEACH, 4.5);

        List<Destination> out = destinationRepository.findAll(
                DestinationSpecifications.withOptionalFilters(Destination.Category.MOUNTAIN, 4.0, 5.0));

        assertThat(out).extracting(Destination::getName).containsExactlyInAnyOrder("ok");
    }

    @Test
    void specification_noFilters_returnsAll() {
        persist("a", Destination.Category.BEACH, 1.0);
        persist("b", Destination.Category.CITY, 2.0);
        persist("c", Destination.Category.HISTORICAL, 3.0);

        List<Destination> out = destinationRepository.findAll(
                DestinationSpecifications.withOptionalFilters(null, null, null));

        assertThat(out).hasSize(3);
    }

    private void persist(String name, Destination.Category category, double rating) {
        Destination d = new Destination();
        d.setName(name);
        d.setCountry("T");
        d.setDescription("desc");
        d.setCategory(category);
        d.setStatus(Destination.Status.ACTIVE);
        d.setRating(rating);
        destinationRepository.save(d);
    }
}
