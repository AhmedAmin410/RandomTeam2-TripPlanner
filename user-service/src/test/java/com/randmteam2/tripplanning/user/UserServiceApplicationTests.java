package com.randmteam2.tripplanning.user;

import com.randmteam2.tripplanning.contracts.feign.BookingServiceClient;
import com.randmteam2.tripplanning.contracts.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.contracts.feign.UserServiceClient;
import com.randmteam2.tripplanning.user.repository.AuthEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserServiceApplicationTests {

    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @MockBean
    RedisTemplate<String, Object> redisTemplate;

    @MockBean
    AuthEventRepository authEventRepository;

    @MockBean
    UserServiceClient userServiceClient;

    @MockBean
    ItineraryServiceClient itineraryServiceClient;

    @MockBean
    DestinationServiceClient destinationServiceClient;

    @MockBean
    BookingServiceClient bookingServiceClient;

    @Test
    void contextLoads() {
    }

}
