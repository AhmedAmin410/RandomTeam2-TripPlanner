package com.randmteam2.tripplanning.itinerary;

import com.randmteam2.tripplanning.itinerary.feign.BookingServiceClient;
import com.randmteam2.tripplanning.itinerary.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.itinerary.feign.UserServiceClient;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEventRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.DestinationNodeRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.UserNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private DestinationServiceClient destinationServiceClient;

    @MockBean
    private BookingServiceClient bookingServiceClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

}
