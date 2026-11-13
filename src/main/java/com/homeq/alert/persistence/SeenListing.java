package com.homeq.alert.persistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name="seen_listings")
public class SeenListing {
    @Id private String id;
    private Instant firstSeenAt;
    protected SeenListing() { }
    public SeenListing(String id) { this.id=id; this.firstSeenAt=Instant.now(); }
    public String getId(){return id;}
    public Instant getFirstSeenAt(){return firstSeenAt;}
}
