package com.homeq.alert.service;
import com.homeq.alert.config.AlertProperties;
import com.homeq.alert.filter.ApartmentFilter;
import com.homeq.alert.model.ApartmentListing;
import com.homeq.alert.notify.Notifier;
import com.homeq.alert.persistence.SeenListing;
import com.homeq.alert.persistence.SeenListingRepository;
import com.homeq.alert.source.HomeQListingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HomeQMonitor {
    private static final Logger log=LoggerFactory.getLogger(HomeQMonitor.class);
    private final HomeQListingSource source;
    private final ApartmentFilter filter;
    private final SeenListingRepository seen;
    private final Notifier notifier;
    private final AlertProperties properties;
    public HomeQMonitor(HomeQListingSource source,ApartmentFilter filter,SeenListingRepository seen,
                        Notifier notifier,AlertProperties properties){
        this.source=source;this.filter=filter;this.seen=seen;this.notifier=notifier;this.properties=properties;
    }
    /** Outcome of one check: listings read from HomeQ, alerts sent, alerts that failed on every channel. */
    public record CheckResult(int read,int alerted,int failed){}

    /** Long-running mode: repeats every poll interval (scheduling is off in run-once mode). */
    @Scheduled(fixedDelayString="${homeq.poll-interval-ms:600000}",initialDelayString="${homeq.initial-delay-ms:15000}")
    public void checkForNewListings(){
        runCheck();
    }

    public CheckResult runCheck(){
        log.info("Checking configured HomeQ search page...");
        List<ApartmentListing> all=source.fetchListings();
        List<ApartmentListing> matching=all.stream().filter(filter::matches).toList();

        // First run with an empty DB: record what's already there without alerting, to avoid a flood
        // of messages for listings that existed before monitoring started. Only baseline when something
        // was actually read, so a failed/misconfigured first poll doesn't count as the baseline.
        if(properties.isBaselineOnFirstRun() && !matching.isEmpty() && seen.count()==0){
            matching.forEach(l->seen.save(new SeenListing(l.id())));
            log.info("First run: recorded {} existing listing(s) as baseline without sending alerts.",matching.size());
            return new CheckResult(all.size(),0,0);
        }

        int alerted=0,failed=0;
        for(ApartmentListing listing:matching){
            if(seen.existsById(listing.id())) continue;
            String subject="New HomeQ apartment: %s, %s, %s".formatted(listing.title(),listing.rooms(),listing.rent());
            String message=("New HomeQ apartment found!\n%s\nArea: %s\nRooms: %s\nRent: %s\n%s")
                .formatted(listing.title(),listing.area(),listing.rooms(),listing.rent(),listing.url());
            try{
                notifier.send(subject,message);
                seen.save(new SeenListing(listing.id()));
                alerted++;
                log.info("Alert sent for {}",listing.url());
            }catch(Exception e){
                failed++;
                log.error("Notification failed for {}. It will be retried next poll.",listing.url(),e);
            }
        }
        log.info("Check done: {} read, {} matching, {} new alert(s) sent, {} failed.",all.size(),matching.size(),alerted,failed);
        return new CheckResult(all.size(),alerted,failed);
    }
}
