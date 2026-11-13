package com.homeq.alert.filter;
import com.homeq.alert.config.AlertProperties;
import com.homeq.alert.model.ApartmentListing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApartmentFilterTest {
    private static ApartmentListing listing(String area,String rent,String rooms){
        return new ApartmentListing("1","Fyrklöversgatan 40",area,rent,rooms,"https://example.test/1");
    }

    @Test void allowsAllWhenNoFiltersConfigured(){
        AlertProperties p=new AlertProperties();
        assertTrue(new ApartmentFilter(p).matches(listing("Gothenburg","10 000 kr/mån","1 rum • 30m2")));
    }
    @Test void matchesAreaIgnoringCase(){
        AlertProperties p=new AlertProperties();p.setAllowedAreas("Södra Änggården,Marklandsgatan");
        ApartmentFilter f=new ApartmentFilter(p);
        assertTrue(f.matches(listing("södra änggården","10000","")));
        assertFalse(f.matches(listing("Centrum","10000","")));
    }
    @Test void minRoomsFilter(){
        AlertProperties p=new AlertProperties();p.setMinRooms(3);
        ApartmentFilter f=new ApartmentFilter(p);
        assertTrue(f.matches(listing("Göteborg","13 842 kr/mån","5 rum • 83m2")));
        assertTrue(f.matches(listing("Göteborg","11 000 kr/mån","3 rum • 74m2")));
        assertFalse(f.matches(listing("Göteborg","9 589 kr/mån","2 rum • 58m2")));
        assertFalse(f.matches(listing("Göteborg","7 869 kr/mån","1.5 rum • 30m2 • Ungdom")));
    }
    @Test void maxRentFilterHandlesSwedishThousandsSeparators(){
        AlertProperties p=new AlertProperties();p.setMaxRent(15000);
        ApartmentFilter f=new ApartmentFilter(p);
        assertTrue(f.matches(listing("Göteborg","14 880 kr/mån","3 rum")));
        assertTrue(f.matches(listing("Göteborg","15 000 kr/mån","3 rum")));
        assertFalse(f.matches(listing("Göteborg","22 188 kr/mån","4 rum")));
    }
    @Test void unreadableRoomsOrRentIsLetThrough(){
        AlertProperties p=new AlertProperties();p.setMinRooms(3);p.setMaxRent(15000);
        assertTrue(new ApartmentFilter(p).matches(listing("Göteborg","","")));
    }
    @Test void parsers(){
        assertEquals(1.5,ApartmentFilter.parseRooms("1.5 rum • 30m2").getAsDouble());
        assertEquals(14609,ApartmentFilter.parseRent("14 609 kr/mån").getAsInt());
        assertTrue(ApartmentFilter.parseRent("Pris saknas").isEmpty());
    }
}
