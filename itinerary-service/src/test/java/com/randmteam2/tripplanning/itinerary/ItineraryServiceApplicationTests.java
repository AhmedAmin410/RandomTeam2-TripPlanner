package com.randmteam2.tripplanning.itinerary;

import com.randmteam2.tripplanning.contracts.feign.BookingServiceClient;
import com.randmteam2.tripplanning.contracts.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.contracts.feign.UserServiceClient;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEventRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.DestinationNodeRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.UserNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ItineraryServiceApplicationTests {

    @MockBean
    private ItineraryEventRepository itineraryEventRepository;

    @MockBean
    private UserNodeRepository userNodeRepository;

    @MockBean
    private DestinationNodeRepository destinationNodeRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private ItineraryServiceClient itineraryServiceClient;

    @MockBean
    private DestinationServiceClient destinationServiceClient;

    @MockBean
    private BookingServiceClient bookingServiceClient;

    @Test
    void contextLoads() {
    }

}
