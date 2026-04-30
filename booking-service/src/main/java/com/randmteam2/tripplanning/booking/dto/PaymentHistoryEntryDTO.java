package com.randmteam2.tripplanning.booking.dto;

import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;

import java.time.LocalDateTime;
import java.util.Map;

public class PaymentHistoryEntryDTO {

    private String id;
    private Long bookingId;
    private String action;
    private LocalDateTime timestamp;
    private String method;
    private Double amount;
    private Map<String, Object> details;

    public PaymentHistoryEntryDTO() {}

    public PaymentHistoryEntryDTO(PaymentAuditEvent ev) {
        this.id        = ev.getId();
        this.bookingId = ev.getBookingId();
        this.action    = ev.getAction();
        this.timestamp = ev.getTimestamp();
        this.method    = ev.getMethod();
        this.amount    = ev.getAmount();
        this.details   = ev.getDetails();
    }

    public String getId()                       { return id; }
    public void setId(String id)                { this.id = id; }
    public Long getBookingId()                  { return bookingId; }
    public void setBookingId(Long bookingId)    { this.bookingId = bookingId; }
    public String getAction()                   { return action; }
    public void setAction(String action)        { this.action = action; }
    public LocalDateTime getTimestamp()         { return timestamp; }
    public void setTimestamp(LocalDateTime t)   { this.timestamp = t; }
    public String getMethod()                   { return method; }
    public void setMethod(String method)        { this.method = method; }
    public Double getAmount()                   { return amount; }
    public void setAmount(Double amount)        { this.amount = amount; }
    public Map<String, Object> getDetails()     { return details; }
    public void setDetails(Map<String, Object> d){ this.details = d; }
}
