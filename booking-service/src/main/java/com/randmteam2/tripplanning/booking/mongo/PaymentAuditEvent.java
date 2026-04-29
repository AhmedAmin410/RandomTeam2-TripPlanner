package com.randmteam2.tripplanning.booking.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "payment_audit_trail")
public class PaymentAuditEvent implements MongoEvent {

    @Id
    private String id;
    private Long bookingId;
    private String action;
    private LocalDateTime timestamp;
    private String method;
    private Double amount;
    private Map<String, Object> details;

    public String getId()                         { return id; }
    public void setId(String id)                  { this.id = id; }
    public Long getBookingId()                    { return bookingId; }
    public void setBookingId(Long bookingId)      { this.bookingId = bookingId; }
    public String getAction()                     { return action; }
    public void setAction(String action)          { this.action = action; }
    public LocalDateTime getTimestamp()           { return timestamp; }
    public void setTimestamp(LocalDateTime t)     { this.timestamp = t; }
    public String getMethod()                     { return method; }
    public void setMethod(String method)          { this.method = method; }
    public Double getAmount()                     { return amount; }
    public void setAmount(Double amount)          { this.amount = amount; }
    public Map<String, Object> getDetails()       { return details; }
    public void setDetails(Map<String, Object> d) { this.details = d; }
}