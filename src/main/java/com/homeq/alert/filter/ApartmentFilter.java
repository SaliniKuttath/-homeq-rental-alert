package com.homeq.alert.filter;
import com.homeq.alert.config.AlertProperties;
import com.homeq.alert.model.ApartmentListing;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a listing is worth an alert. Area, minimum rooms and maximum rent are each optional.
 * If rooms or rent can't be read from the card, the listing is let through rather than silently dropped,
 * so a page-layout change causes extra alerts instead of missed ones.
 */
@Component
public class ApartmentFilter {
    private static final Pattern ROOMS=Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*rum",Pattern.CASE_INSENSITIVE);
    private static final Pattern RENT=Pattern.compile("([\\d\\s\\u00A0\\u202F]+)\\s*kr",Pattern.CASE_INSENSITIVE);

    private final AlertProperties properties;
    public ApartmentFilter(AlertProperties properties){this.properties=properties;}

    public boolean matches(ApartmentListing listing){
        return matchesArea(listing) && matchesRooms(listing) && matchesRent(listing);
    }

    private boolean matchesArea(ApartmentListing listing){
        String configured=properties.getAllowedAreas();
        if(configured==null || configured.isBlank()) return true;
        List<String> areas=Arrays.stream(configured.split(",")).map(String::trim).filter(s->!s.isBlank()).toList();
        String actual=(listing.area()+" "+listing.title()).toLowerCase(Locale.ROOT);
        return areas.stream().anyMatch(a->actual.contains(a.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesRooms(ApartmentListing listing){
        double min=properties.getMinRooms();
        if(min<=0) return true;
        OptionalDouble rooms=parseRooms(listing.rooms());
        return rooms.isEmpty() || rooms.getAsDouble()>=min;
    }

    private boolean matchesRent(ApartmentListing listing){
        int max=properties.getMaxRent();
        if(max<=0) return true;
        OptionalInt rent=parseRent(listing.rent());
        return rent.isEmpty() || rent.getAsInt()<=max;
    }

    /** "3 rum • 74m2" -> 3.0, "1.5 rum" -> 1.5 */
    static OptionalDouble parseRooms(String text){
        if(text==null) return OptionalDouble.empty();
        Matcher m=ROOMS.matcher(text);
        return m.find()?OptionalDouble.of(Double.parseDouble(m.group(1).replace(',','.'))):OptionalDouble.empty();
    }

    /** "14 609 kr/mån" -> 14609 */
    static OptionalInt parseRent(String text){
        if(text==null) return OptionalInt.empty();
        Matcher m=RENT.matcher(text);
        if(!m.find()) return OptionalInt.empty();
        String digits=m.group(1).replaceAll("\\D","");
        return digits.isEmpty()?OptionalInt.empty():OptionalInt.of(Integer.parseInt(digits));
    }
}
