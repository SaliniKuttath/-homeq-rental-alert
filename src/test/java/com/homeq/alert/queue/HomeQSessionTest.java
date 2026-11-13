package com.homeq.alert.queue;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class HomeQSessionTest {
    private static String jwt(long exp){
        Base64.Encoder e=Base64.getUrlEncoder().withoutPadding();
        return e.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8))+"."
            +e.encodeToString(("{\"exp\":"+exp+",\"username\":\"x\"}").getBytes(StandardCharsets.UTF_8))+".sig";
    }
    private static String state(String token){
        return "{\"cookies\":[{\"name\":\"homeq_app\",\"value\":\"1\",\"domain\":\"www.homeq.se\"},{\"name\":\"_fbp\",\"value\":\"2\",\"domain\":\".facebook.com\"}],"
            +"\"origins\":[{\"origin\":\"https://www.homeq.se\",\"localStorage\":[{\"name\":\"token\",\"value\":\""+token+"\"},{\"name\":\"id5id\",\"value\":\"ad\"}]},"
            +"{\"origin\":\"https://ads.example\",\"localStorage\":[{\"name\":\"token\",\"value\":\"other\"}]}]}";
    }

    @Test void minimizeKeepsOnlyHomeQLogin(){
        String slim=HomeQSession.minimize(state(jwt(1_800_000_000L)));
        assertTrue(slim.contains("homeq_app"));
        assertTrue(slim.contains("\"token\""));
        assertFalse(slim.contains("_fbp"));
        assertFalse(slim.contains("id5id"));
        assertFalse(slim.contains("ads.example"));
    }
    @Test void readsTokenExpiry(){
        assertEquals(Instant.ofEpochSecond(1_800_000_000L),HomeQSession.tokenExpiryFromJson(state(jwt(1_800_000_000L))).orElseThrow());
    }
    @Test void parsesQueueText(){
        assertEquals(47,QueuePositionReader.parse("Du är nummer 47 i kön").orElseThrow());
        assertTrue(QueuePositionReader.parse("Intresseanmälan lämnad").isEmpty());
    }
}
