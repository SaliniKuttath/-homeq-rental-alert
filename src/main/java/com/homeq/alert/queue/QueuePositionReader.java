package com.homeq.alert.queue;
import com.homeq.alert.model.ApartmentListing;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads "Du är nummer N i kön" from listing pages using the saved HomeQ login (see tools.HomeQLogin).
 * Only that line of the page is read. Optional: without a valid session file, nothing is read.
 */
@Component
public class QueuePositionReader {
    private static final Logger log=LoggerFactory.getLogger(QueuePositionReader.class);
    static final Pattern QUEUE_TEXT=Pattern.compile("Du är nummer\\s+(\\d+)\\s+i kön",Pattern.CASE_INSENSITIVE);  // Playwright only accepts CASE_INSENSITIVE/DOTALL/MULTILINE

    public enum Status{ NOT_SET_UP, VALID, EXPIRED }

    private final Path sessionFile;
    private final boolean headless;

    public QueuePositionReader(@Value("${homeq.session-file:data/homeq-session.json}") String sessionFile,
                               @Value("${homeq.headless:true}") boolean headless){
        this.sessionFile=Path.of(sessionFile);this.headless=headless;
    }

    public Status status(){ return status(Instant.now()); }
    Status status(Instant now){
        Optional<Instant> exp=expiry();
        if(exp.isEmpty()) return Status.NOT_SET_UP;
        return exp.get().isAfter(now)?Status.VALID:Status.EXPIRED;
    }
    public Optional<Instant> expiry(){ return HomeQSession.tokenExpiry(sessionFile); }

    /** Returns listing id -> queue position for the given listings; listings where it couldn't be read are absent. */
    public Map<String,Integer> read(List<ApartmentListing> listings){
        Map<String,Integer> positions=new HashMap<>();
        if(listings.isEmpty() || status()!=Status.VALID) return positions;
        try(Playwright pw=Playwright.create()){
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            try{
                BrowserContext ctx=browser.newContext(new Browser.NewContextOptions().setStorageStatePath(sessionFile));
                Page page=ctx.newPage();
                for(ApartmentListing l:listings){
                    try{
                        page.navigate(l.url(),new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                        Locator line=page.getByText(QUEUE_TEXT).first();
                        line.waitFor(new Locator.WaitForOptions().setTimeout(20000));
                        parse(line.innerText()).ifPresent(n->positions.put(l.id(),n));
                    }catch(PlaywrightException e){
                        log.warn("Couldn't read queue position for {}: {}",l.url(),e.getMessage().lines().findFirst().orElse(""));
                    }
                }
            }finally{ browser.close(); }
        }catch(RuntimeException e){
            log.warn("Queue position lookup failed: {}",e.getMessage());
        }
        return positions;
    }

    static Optional<Integer> parse(String text){
        Matcher m=QUEUE_TEXT.matcher(text==null?"":text);
        return m.find()?Optional.of(Integer.parseInt(m.group(1))):Optional.empty();
    }
}
