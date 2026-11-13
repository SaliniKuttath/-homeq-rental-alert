package com.homeq.alert.tools;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens a normal (visible) browser on homeq.se so YOU can log in yourself (password or BankID).
 * The app never sees your password: it only saves the browser's login cookies to
 * data/homeq-session.json, which the alert checker reuses to read your queue position.
 *
 * Run:  mvn -q exec:java -Dexec.mainClass=com.homeq.alert.tools.HomeQLogin
 * Then log in in the browser, leave it open, and press Enter in the console.
 * Treat data/homeq-session.json like a password: it's git-ignored; never share it.
 */
public final class HomeQLogin {
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args.length>0?args[0]:"data/homeq-session.json");
        Files.createDirectories(out.toAbsolutePath().getParent());
        try(Playwright pw=Playwright.create()){
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext ctx=browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
            Page page=ctx.newPage();
            page.navigate("https://www.homeq.se/");
            Path done=out.resolveSibling("login-done");
            Files.deleteIfExists(done);
            System.out.println("A browser window is open. Log in to HomeQ there and leave the window OPEN.");
            System.out.println("When you're logged in, press Enter here (or create the file "+done.toAbsolutePath()+").");
            // Don't touch the page while you log in (polling it disturbs HomeQ's login form).
            // Wait for Enter on the console, or for the signal file, without talking to the browser.
            Thread enter=new Thread(()->{ try{ if(System.in.read()>=0) Files.writeString(done,"enter"); }catch(Exception ignored){} });
            enter.setDaemon(true); enter.start();
            while(!Files.exists(done)) Thread.sleep(1000);
            // Keep only the HomeQ login (cookies + token), not the dozens of ad/tracker entries.
            String full=ctx.storageState();
            Files.writeString(out,com.homeq.alert.queue.HomeQSession.minimize(full),java.nio.charset.StandardCharsets.UTF_8);
            Files.deleteIfExists(done);
            browser.close();
        }
        System.out.println("Saved HomeQ login session to "+out.toAbsolutePath()+". You can close this window.");
        com.homeq.alert.queue.HomeQSession.tokenExpiry(out).ifPresentOrElse(
            exp->System.out.println("Login is valid until "+exp+" (about 7 days). Run this again before then."),
            ()->System.out.println("WARNING: no HomeQ login token found. Did you finish logging in?"));
    }
}
