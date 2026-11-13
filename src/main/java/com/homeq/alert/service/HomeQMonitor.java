package com.homeq.alert.service;
import com.homeq.alert.config.AlertProperties;
import com.homeq.alert.filter.ApartmentFilter;
import com.homeq.alert.model.ApartmentListing;
import com.homeq.alert.notify.Notifier;
import com.homeq.alert.persistence.SeenListing;
import com.homeq.alert.persistence.SeenListingRepository;
import com.homeq.alert.queue.QueuePositionReader;
import com.homeq.alert.source.HomeQListingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class HomeQMonitor {
    private static final Logger log=LoggerFactory.getLogger(HomeQMonitor.class);
    private static final DateTimeFormatter WHEN=DateTimeFormatter.ofPattern("EEE d MMM HH:mm").withZone(ZoneId.of("Europe/Stockholm"));
    private final HomeQListingSource source;
    private final ApartmentFilter filter;
    private final SeenListingRepository seen;
    private final Notifier notifier;
    private final AlertProperties properties;
    private final QueuePositionReader queue;

    public HomeQMonitor(HomeQListingSource source,ApartmentFilter filter,SeenListingRepository seen,
                        Notifier notifier,AlertProperties properties,QueuePositionReader queue){
        this.source=source;this.filter=filter;this.seen=seen;this.notifier=notifier;this.properties=properties;this.queue=queue;
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
        remindIfLoginExpiring();
        log.info("HomeQ login for queue positions: {}{}",queue.status(),queue.expiry().map(e->" (until "+e+")").orElse(""));
        List<ApartmentListing> all=source.fetchListings();
        List<ApartmentListing> matching=all.stream().filter(filter::matches).toList();

        // First run with an empty DB: record what's already there without alerting, to avoid a flood
        // of messages for listings that existed before monitoring started. Only baseline when something
        // was actually read, so a failed/misconfigured first poll doesn't count as the baseline.
        if(properties.isBaselineOnFirstRun() && !matching.isEmpty() && countListings()==0){
            matching.forEach(l->seen.save(new SeenListing(l.id())));
            log.info("First run: recorded {} existing listing(s) as baseline without sending alerts.",matching.size());
            return new CheckResult(all.size(),0,0);
        }

        List<ApartmentListing> fresh=matching.stream().filter(l->!seen.existsById(l.id())).toList();
        Map<String,Integer> positions=fresh.isEmpty()?Map.of():queue.read(fresh);
        QueuePositionReader.Status queueStatus=queue.status();

        int alerted=0,failed=0;
        for(ApartmentListing listing:fresh){
            Integer pos=positions.get(listing.id());
            String queueLine=switch(queueStatus){
                case NOT_SET_UP -> "";
                case EXPIRED -> "Your queue position: unknown (HomeQ login expired, please log in again)\n";
                case VALID -> pos!=null?"Your queue position: "+pos+"\n":"Your queue position: couldn't be read\n";
            };
            String subject=(pos!=null?"#"+pos+" in queue: ":"New HomeQ apartment: ")
                +"%s, %s, %s".formatted(listing.title(),listing.rooms(),listing.rent());
            String message=("New HomeQ apartment found!\n%s\nArea: %s\nRooms: %s\nRent: %s\n%s%s")
                .formatted(listing.title(),listing.area(),listing.rooms(),listing.rent(),queueLine,listing.url());
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

    /** Number of real listings seen (excludes reminder markers stored in the same table). */
    private long countListings(){
        return seen.findAll().stream().filter(s->!s.getId().startsWith(REMINDER_PREFIX)).count();
    }

    static final String REMINDER_PREFIX="reminder:";

    /** Emails once per login when the saved HomeQ login has less than a day left (or has expired). */
    private void remindIfLoginExpiring(){
        queue.expiry().ifPresent(exp->{
            if(Duration.between(Instant.now(),exp).compareTo(Duration.ofHours(24))>0) return;
            String marker=REMINDER_PREFIX+"login-expiry:"+exp.getEpochSecond();
            if(seen.existsById(marker)) return;
            boolean expired=!exp.isAfter(Instant.now());
            String subject=expired?"HomeQ alert: your HomeQ login has expired":"HomeQ alert: your HomeQ login expires "+WHEN.format(exp);
            String body=(expired?"Your saved HomeQ login has expired":"Your saved HomeQ login expires on "+WHEN.format(exp))
                +", so alerts can't show your queue position until you log in again.\n\n"
                +"Apartment alerts keep working. To renew, open PowerShell in the project folder and run:\n"
                +"  powershell -ExecutionPolicy Bypass -File scripts\\update-homeq-login.ps1\n";
            try{
                notifier.send(subject,body);
                seen.save(new SeenListing(marker));
                log.info("Sent HomeQ login expiry reminder.");
            }catch(Exception e){
                log.warn("Couldn't send login expiry reminder: {}",e.getMessage());
            }
        });
    }
}
