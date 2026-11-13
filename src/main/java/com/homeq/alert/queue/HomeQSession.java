package com.homeq.alert.queue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Helpers for the saved HomeQ login (a Playwright "storage state" JSON file).
 * HomeQ keeps the login as a JWT named "token" in www.homeq.se localStorage; it lasts 7 days and is not renewed.
 */
public final class HomeQSession {
    private HomeQSession(){}

    /** Keeps only what's needed to be logged in (homeq.se cookies + the localStorage "token"), dropping ad/tracker data. */
    public static String minimize(String storageStateJson){
        JsonObject in=JsonParser.parseString(storageStateJson).getAsJsonObject();
        JsonObject out=new JsonObject();
        JsonArray cookies=new JsonArray();
        for(JsonElement c:in.getAsJsonArray("cookies"))
            if(c.getAsJsonObject().get("domain").getAsString().contains("homeq.se")) cookies.add(c);
        out.add("cookies",cookies);
        JsonArray origins=new JsonArray();
        for(JsonElement o:in.getAsJsonArray("origins")){
            JsonObject origin=o.getAsJsonObject();
            if(!origin.get("origin").getAsString().contains("homeq.se")) continue;
            JsonArray kept=new JsonArray();
            for(JsonElement kv:origin.getAsJsonArray("localStorage"))
                if("token".equals(kv.getAsJsonObject().get("name").getAsString())) kept.add(kv);
            if(kept.isEmpty()) continue;
            JsonObject slim=new JsonObject();
            slim.addProperty("origin",origin.get("origin").getAsString());
            slim.add("localStorage",kept);
            origins.add(slim);
        }
        out.add("origins",origins);
        return new Gson().toJson(out);
    }

    /** Expiry of the login token in the session file, if the file exists and has one. */
    public static Optional<Instant> tokenExpiry(Path sessionFile){
        try{
            if(!Files.exists(sessionFile)) return Optional.empty();
            return tokenExpiryFromJson(Files.readString(sessionFile,StandardCharsets.UTF_8));
        }catch(IOException|RuntimeException e){
            return Optional.empty();
        }
    }

    static Optional<Instant> tokenExpiryFromJson(String storageStateJson){
        JsonObject state=JsonParser.parseString(storageStateJson).getAsJsonObject();
        for(JsonElement o:state.getAsJsonArray("origins")){
            for(JsonElement kv:o.getAsJsonObject().getAsJsonArray("localStorage")){
                JsonObject e=kv.getAsJsonObject();
                if(!"token".equals(e.get("name").getAsString())) continue;
                String[] parts=e.get("value").getAsString().split("\\.");
                if(parts.length!=3) return Optional.empty();
                JsonObject claims=JsonParser.parseString(new String(Base64.getUrlDecoder().decode(parts[1]),StandardCharsets.UTF_8)).getAsJsonObject();
                return claims.has("exp")?Optional.of(Instant.ofEpochSecond(claims.get("exp").getAsLong())):Optional.empty();
            }
        }
        return Optional.empty();
    }
}
