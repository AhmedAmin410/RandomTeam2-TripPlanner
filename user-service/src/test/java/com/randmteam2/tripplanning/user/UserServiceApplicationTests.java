package com.randmteam2.tripplanning.user;

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

    @Test
    void contextLoads() {
    }

}
