package com.homeq.alert.notify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sends each alert through every channel listed in {@code alert.channels} (e.g. "email,callmebot").
 * The alert counts as delivered if at least one channel succeeds, so one broken channel doesn't
 * cause repeat alerts on the others; failures are logged.
 */
@Component
public class MultiChannelNotifier implements Notifier {
    private static final Logger log=LoggerFactory.getLogger(MultiChannelNotifier.class);
    private final List<AlertChannel> enabled;

    public MultiChannelNotifier(List<AlertChannel> available,@Value("${alert.channels:email}") String channels){
        Map<String,AlertChannel> byName=available.stream().collect(Collectors.toMap(AlertChannel::name,Function.identity()));
        List<AlertChannel> list=new ArrayList<>();
        for(String name:Arrays.stream(channels.split(",")).map(s->s.trim().toLowerCase(Locale.ROOT)).filter(s->!s.isEmpty()).toList()){
            AlertChannel c=byName.get(name);
            if(c==null) throw new IllegalArgumentException("Unknown alert channel '"+name+"'. Valid: "+byName.keySet());
            list.add(c);
        }
        if(list.isEmpty()) throw new IllegalArgumentException("ALERT_CHANNELS is empty; set e.g. ALERT_CHANNELS=email,callmebot");
        this.enabled=List.copyOf(list);
        log.info("Alert channels enabled: {}",enabled.stream().map(AlertChannel::name).toList());
    }

    @Override public void send(String subject,String body){
        List<String> failures=new ArrayList<>();
        for(AlertChannel c:enabled){
            try{
                c.send(subject,body);
            }catch(RuntimeException e){
                log.warn("Alert channel '{}' failed: {}",c.name(),e.getMessage());
                failures.add(c.name()+": "+e.getMessage());
            }
        }
        if(failures.size()==enabled.size())
            throw new IllegalStateException("All alert channels failed: "+failures);
    }
}
