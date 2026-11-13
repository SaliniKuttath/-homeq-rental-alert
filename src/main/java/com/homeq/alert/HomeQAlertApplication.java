package com.homeq.alert;
import com.homeq.alert.service.HomeQMonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class HomeQAlertApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx=SpringApplication.run(HomeQAlertApplication.class, args);
        if(ctx.getEnvironment().getProperty("homeq.run-once",Boolean.class,false)){
            // Run-once mode (GitHub Actions / cron): one check, then exit.
            // Exit code 1 if nothing could be read or an alert failed, so the scheduler reports the run as failed.
            HomeQMonitor.CheckResult r=ctx.getBean(HomeQMonitor.class).runCheck();
            int code=(r.read()==0||r.failed()>0)?1:0;
            System.exit(SpringApplication.exit(ctx,()->code));
        }
    }
}
