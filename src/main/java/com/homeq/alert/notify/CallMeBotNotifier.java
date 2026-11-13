package com.homeq.alert.notify;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Free WhatsApp messages to your own number via CallMeBot (unofficial, personal use only).
 * Setup: https://www.callmebot.com/blog/free-api-whatsapp-messages/
 */
@Component
public class CallMeBotNotifier implements AlertChannel {
    private static final Duration TIMEOUT=Duration.ofSeconds(30);
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final String phone;
    private final String apiKey;

    public CallMeBotNotifier(@Value("${callmebot.phone:}") String phone,@Value("${callmebot.api-key:}") String apiKey){
        this.phone=phone.replaceAll("[\\s-]","");this.apiKey=apiKey.trim();
    }

    @Override public String name(){return "callmebot";}

    @Override public void send(String subject,String body){
        if(phone.isBlank()||apiKey.isBlank())
            throw new IllegalStateException("CallMeBot needs CALLMEBOT_PHONE and CALLMEBOT_API_KEY in the environment.");
        HttpRequest request=HttpRequest.newBuilder().uri(buildUri(phone,body,apiKey)).timeout(TIMEOUT).GET().build();
        try{
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            String text=response.body()==null?"":response.body();
            String lower=text.toLowerCase(Locale.ROOT);
            // CallMeBot often answers 200 with an HTML page even on errors, so also check the text.
            if(response.statusCode()<200||response.statusCode()>=300||lower.contains("apikey is invalid")||lower.contains("error"))
                throw new IllegalStateException("CallMeBot HTTP "+response.statusCode()+": "+text.replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim());
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("CallMeBot send interrupted",e);}
        catch(java.io.IOException e){throw new IllegalStateException("CallMeBot send failed",e);}
    }

    static URI buildUri(String phone,String text,String apiKey){
        return URI.create("https://api.callmebot.com/whatsapp.php?phone="+enc(phone)
            +"&text="+enc(text)+"&apikey="+enc(apiKey));
    }
    /** Percent-encodes, using %20 for spaces (CallMeBot's docs) and %2B for '+' in phone numbers. */
    private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8).replace("+","%20");}
}
