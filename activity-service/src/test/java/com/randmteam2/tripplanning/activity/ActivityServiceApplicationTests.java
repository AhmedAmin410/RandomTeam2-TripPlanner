package com.randmteam2.tripplanning.activity;

import com.randmteam2.tripplanning.activity.repository.ActivityEventRepository;
import com.randmteam2.tripplanning.activity.repository.ActivityLifecycleEventRepository;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ActivityServiceApplicationTests {

    @MockBean
    private ActivityLifecycleEventRepository activityLifecycleEventRepository;

    @MockBean
    private ActivityEventRepository activityEventRepository;

    @MockBean
    private ItineraryServiceClient itineraryServiceClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

}
