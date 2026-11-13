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
import java.util.Base64;

/**
 * Sends alerts through Twilio WhatsApp. Note: Twilio trial accounts only allow Twilio's own templates
 * (error 21654), so free-text alerts need an upgraded account with an approved sender/template.
 */
@Component
public class WhatsAppNotifier implements AlertChannel {
    @Override public String name(){return "twilio";}

    @Value("${twilio.account-sid:}") private String accountSid;
    @Value("${twilio.auth-token:}") private String authToken;
    @Value("${twilio.whatsapp-from:}") private String from;
    @Value("${twilio.whatsapp-to:}") private String to;
    private static final Duration TIMEOUT=Duration.ofSeconds(30);
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    @Override public void send(String subject,String message){
        if(accountSid.isBlank()||authToken.isBlank()||from.isBlank()||to.isBlank())
            throw new IllegalStateException("Twilio WhatsApp environment variables are not configured.");
        String body="From="+enc(from)+"&To="+enc(to)+"&Body="+enc(message);
        String basic=Base64.getEncoder().encodeToString((accountSid+":"+authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest request=HttpRequest.newBuilder()
            .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/"+accountSid+"/Messages.json"))
            .header("Authorization","Basic "+basic)
            .header("Content-Type","application/x-www-form-urlencoded")
            .timeout(TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try{
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()<200||response.statusCode()>=300)
                throw new IllegalStateException("Twilio HTTP "+response.statusCode()+": "+response.body());
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("WhatsApp send interrupted",e);}
        catch(java.io.IOException e){throw new IllegalStateException("WhatsApp send failed",e);}
    }
    private String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
