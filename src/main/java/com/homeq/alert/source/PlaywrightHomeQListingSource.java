package com.homeq.alert.source;
import com.homeq.alert.config.AlertProperties;
import com.homeq.alert.model.ApartmentListing;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlaywrightHomeQListingSource implements HomeQListingSource {
    private static final Logger log=LoggerFactory.getLogger(PlaywrightHomeQListingSource.class);
    private final AlertProperties properties;
    public PlaywrightHomeQListingSource(AlertProperties properties){this.properties=properties;}

    @Override public List<ApartmentListing> fetchListings() {
        if(properties.getCardSelector()==null || properties.getCardSelector().isBlank()){
            log.warn("HOMEQ_CARD_SELECTOR is empty. Inspect HomeQ results and configure selectors; no listings were read.");
            return List.of();
        }
        // A listing can appear twice (promoted + regular, or on two pages), so de-duplicate by URL.
        Map<String,ApartmentListing> byUrl=new LinkedHashMap<>();
        try(Playwright playwright=Playwright.create()){
            Browser browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(properties.isHeadless()));
            try{
                Page page=browser.newPage();
                int maxPages=Math.max(1,properties.getMaxPages());
                for(int n=1;n<=maxPages;n++){
                    // Page 1 gets the full wait; later pages are usually fast or empty, so don't wait as long.
                    long wait=n==1?properties.getCardWaitTimeoutMs():Math.min(properties.getCardWaitTimeoutMs(),10000);
                    int before=byUrl.size();
                    int found=readPage(page,pageUrl(properties.getSearchUrl(),n),byUrl,wait);
                    if(found==0 && n==1) log.warn("No elements matching HOMEQ_CARD_SELECTOR '{}' appeared within {} ms. "
                        +"Either there are no listings right now or the selector is out of date.",
                        properties.getCardSelector(),wait);
                    if(found==0 || byUrl.size()==before) break;   // empty page, or nothing new: past the last page
                }
            } finally { browser.close(); }
        } catch(Exception e){log.error("HomeQ page read failed. Check URL, selectors and permitted access.",e);}
        log.info("Read {} unique listing(s) from HomeQ.",byUrl.size());
        return new ArrayList<>(byUrl.values());
    }

    /** Loads one results page and adds its cards to {@code byUrl}. Returns the number of cards on the page. */
    private int readPage(Page page,String pageUrl,Map<String,ApartmentListing> byUrl,long waitMs){
        page.navigate(pageUrl,new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
        Locator cards=page.locator(properties.getCardSelector());
        try{
            cards.first().waitFor(new Locator.WaitForOptions().setTimeout(waitMs));
        }catch(TimeoutError e){
            return 0;
        }
        int count=cards.count();
        for(int i=0;i<count;i++){
            Locator card=cards.nth(i);
            String href=attribute(card,properties.getLinkSelector(),"href");
            if(href==null || href.isBlank()) continue;
            String url=canonicalUrl(URI.create(pageUrl).resolve(href));
            String title=text(card,properties.getTitleSelector());
            if(title.isBlank()) title="HomeQ apartment";
            byUrl.putIfAbsent(url,new ApartmentListing(url,title,text(card,properties.getAreaSelector()),
                text(card,properties.getPriceSelector()),text(card,properties.getRoomsSelector()),url));
        }
        return count;
    }

    /** Page 1 is the configured URL as-is; later pages add HomeQ's {@code page=N} parameter. */
    static String pageUrl(String searchUrl,int n){
        if(n<=1) return searchUrl;
        return searchUrl+(searchUrl.contains("?")?"&":"?")+"page="+n;
    }
    /** Drops the query string and fragment: HomeQ adds tracking params (ht_position, boost_value...) that change on every load. */
    static String canonicalUrl(URI uri){
        try{
            return new URI(uri.getScheme(),uri.getAuthority(),uri.getPath(),null,null).toString();
        }catch(java.net.URISyntaxException e){
            return uri.toString();
        }
    }
    private String text(Locator card,String selector){
        if(selector==null || selector.isBlank()) return "";
        Locator node=card.locator(selector).first();
        return node.count()>0?node.innerText().trim():"";
    }
    private String attribute(Locator card,String selector,String name){
        if(selector==null || selector.isBlank()) return null;
        Locator node=card.locator(selector).first();
        return node.count()>0?node.getAttribute(name):null;
    }
}
