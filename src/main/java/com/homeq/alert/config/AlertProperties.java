package com.homeq.alert.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "homeq")
public class AlertProperties {
    private String searchUrl = "https://www.homeq.se/";
    private String cardSelector = "";
    private String titleSelector = "";
    private String priceSelector = "";
    private String areaSelector = "";
    private String linkSelector = "a";
    private boolean headless = true;
    private String allowedAreas = "";
    private boolean baselineOnFirstRun = true;
    private long cardWaitTimeoutMs = 30000;
    private String roomsSelector = "";
    private double minRooms = 0;
    private int maxRent = 0;
    private int maxPages = 1;

    public String getSearchUrl(){return searchUrl;} public void setSearchUrl(String v){searchUrl=v;}
    public String getCardSelector(){return cardSelector;} public void setCardSelector(String v){cardSelector=v;}
    public String getTitleSelector(){return titleSelector;} public void setTitleSelector(String v){titleSelector=v;}
    public String getPriceSelector(){return priceSelector;} public void setPriceSelector(String v){priceSelector=v;}
    public String getAreaSelector(){return areaSelector;} public void setAreaSelector(String v){areaSelector=v;}
    public String getLinkSelector(){return linkSelector;} public void setLinkSelector(String v){linkSelector=v;}
    public boolean isHeadless(){return headless;} public void setHeadless(boolean v){headless=v;}
    public String getAllowedAreas(){return allowedAreas;} public void setAllowedAreas(String v){allowedAreas=v;}
    public boolean isBaselineOnFirstRun(){return baselineOnFirstRun;} public void setBaselineOnFirstRun(boolean v){baselineOnFirstRun=v;}
    public long getCardWaitTimeoutMs(){return cardWaitTimeoutMs;} public void setCardWaitTimeoutMs(long v){cardWaitTimeoutMs=v;}
    public String getRoomsSelector(){return roomsSelector;} public void setRoomsSelector(String v){roomsSelector=v;}
    public double getMinRooms(){return minRooms;} public void setMinRooms(double v){minRooms=v;}
    public int getMaxRent(){return maxRent;} public void setMaxRent(int v){maxRent=v;}
    public int getMaxPages(){return maxPages;} public void setMaxPages(int v){maxPages=v;}
}
