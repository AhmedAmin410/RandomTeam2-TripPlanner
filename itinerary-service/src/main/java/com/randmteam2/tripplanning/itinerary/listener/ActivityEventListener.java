package com.randmteam2.tripplanning.itinerary.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ActivityEventListener {

    @RabbitListener(queues = "activity.itinerary.saga-listener")
    public void handleItineraryCompleted(String message) {
        // Logic to write COMPLETED rows in Cassandra
        System.out.println("Writing COMPLETED rows in Cassandra for: " + message);
        // Publish activity.lifecycle-recorded
        System.out.println("Publishing activity.lifecycle-recorded for: " + message);
    }

    @RabbitListener(queues = "activity.itinerary.saga-listener")
    public void handleItineraryCancelled(String message) {
        // Logic to write CANCELLED rows in Cassandra
        System.out.println("Writing CANCELLED rows in Cassandra for: " + message);
        // Publish activity.cancelled
        System.out.println("Publishing activity.cancelled for: " + message);
    }
}