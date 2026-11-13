package com.homeq.alert.source;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class PlaywrightHomeQListingSourceTest {
    @Test void canonicalUrlStripsTrackingParams(){
        String a=PlaywrightHomeQListingSource.canonicalUrl(URI.create(
            "https://www.homeq.se/lagenhet/278656-4rum-almhult?ht_source=individual.278656&ht_position=2&boost_value=24.1"));
        String b=PlaywrightHomeQListingSource.canonicalUrl(URI.create(
            "https://www.homeq.se/lagenhet/278656-4rum-almhult?ht_source=individual.278656&ht_position=7&boost_value=11.3"));
        assertEquals("https://www.homeq.se/lagenhet/278656-4rum-almhult",a);
        assertEquals(a,b);
    }
    @Test void pageUrlAppendsPageParam(){
        String base="https://www.homeq.se/search?selectedShapes=face.40,face.44";
        assertEquals(base,PlaywrightHomeQListingSource.pageUrl(base,1));
        assertEquals(base+"&page=2",PlaywrightHomeQListingSource.pageUrl(base,2));
        assertEquals("https://www.homeq.se/search?page=3",PlaywrightHomeQListingSource.pageUrl("https://www.homeq.se/search",3));
    }
}
