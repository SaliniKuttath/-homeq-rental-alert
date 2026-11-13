package com.homeq.alert.notify;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NotifierSelectionTest {
    /** Records calls; optionally fails. */
    static class FakeChannel implements AlertChannel {
        final String name; final boolean fail; final List<String> sent=new ArrayList<>();
        FakeChannel(String name,boolean fail){this.name=name;this.fail=fail;}
        @Override public String name(){return name;}
        @Override public void send(String subject,String body){ if(fail) throw new IllegalStateException(name+" down"); sent.add(body); }
    }

    @Test void sendsToEveryEnabledChannel(){
        FakeChannel email=new FakeChannel("email",false), bot=new FakeChannel("callmebot",false), twilio=new FakeChannel("twilio",false);
        new MultiChannelNotifier(List.of(email,bot,twilio)," Email , callmebot ").send("s","hello");
        assertEquals(List.of("hello"),email.sent);
        assertEquals(List.of("hello"),bot.sent);
        assertTrue(twilio.sent.isEmpty());
    }
    @Test void oneFailingChannelStillCountsAsDelivered(){
        FakeChannel email=new FakeChannel("email",false), bot=new FakeChannel("callmebot",true);
        assertDoesNotThrow(()->new MultiChannelNotifier(List.of(email,bot),"email,callmebot").send("s","hi"));
        assertEquals(1,email.sent.size());
    }
    @Test void allChannelsFailingThrowsSoListingIsRetried(){
        FakeChannel email=new FakeChannel("email",true), bot=new FakeChannel("callmebot",true);
        assertThrows(IllegalStateException.class,()->new MultiChannelNotifier(List.of(email,bot),"email,callmebot").send("s","hi"));
    }
    @Test void unknownChannelIsRejectedAtStartup(){
        assertThrows(IllegalArgumentException.class,()->new MultiChannelNotifier(List.of(new FakeChannel("email",false)),"email,telegram"));
    }
    @Test void callMeBotUrlIsEncoded(){
        String uri=CallMeBotNotifier.buildUri("+919645814799","New apartment!\nRent: 13 842 kr","abc123").toString();
        assertEquals("https://api.callmebot.com/whatsapp.php?phone=%2B919645814799&text=New%20apartment%21%0ARent%3A%2013%20842%20kr&apikey=abc123",uri);
    }
}
