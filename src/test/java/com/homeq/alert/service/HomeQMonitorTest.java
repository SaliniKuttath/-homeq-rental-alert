package com.homeq.alert.service;
import com.homeq.alert.config.AlertProperties;
import com.homeq.alert.filter.ApartmentFilter;
import com.homeq.alert.model.ApartmentListing;
import com.homeq.alert.notify.Notifier;
import com.homeq.alert.persistence.SeenListing;
import com.homeq.alert.persistence.SeenListingRepository;
import com.homeq.alert.source.HomeQListingSource;
import com.homeq.alert.queue.QueuePositionReader;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HomeQMonitorTest {
    private HomeQListingSource source;
    private SeenListingRepository seen;
    private Notifier notifier;
    private AlertProperties props;
    private HomeQMonitor monitor;
    private QueuePositionReader queue;

    private static ApartmentListing listing(String id){
        return new ApartmentListing(id,"Apartment "+id,"Högsbo","9000 kr","3 rum",id);
    }

    @BeforeEach void setUp(){
        source=mock(HomeQListingSource.class);
        seen=mock(SeenListingRepository.class);
        notifier=mock(Notifier.class);
        props=new AlertProperties();
        queue=mock(QueuePositionReader.class);
        when(queue.status()).thenReturn(QueuePositionReader.Status.NOT_SET_UP);
        monitor=new HomeQMonitor(source,new ApartmentFilter(props),seen,notifier,props,queue);
    }

    @Test void firstRunRecordsBaselineWithoutNotifying(){
        when(source.fetchListings()).thenReturn(List.of(listing("a"),listing("b")));
        when(seen.findAll()).thenReturn(List.of());
        monitor.checkForNewListings();
        verify(notifier,never()).send(anyString(),anyString());
        verify(seen,times(2)).save(any(SeenListing.class));
    }

    @Test void emptyFirstFetchDoesNotCountAsBaseline(){
        when(source.fetchListings()).thenReturn(List.of());
        when(seen.findAll()).thenReturn(List.of());
        monitor.checkForNewListings();
        verify(seen,never()).save(any());
        verify(notifier,never()).send(anyString(),anyString());
    }

    @Test void laterRunsNotifyOnlyUnseenListings(){
        when(source.fetchListings()).thenReturn(List.of(listing("a"),listing("b")));
        when(seen.findAll()).thenReturn(List.of(new SeenListing("x")));
        when(seen.existsById("a")).thenReturn(true);
        when(seen.existsById("b")).thenReturn(false);
        monitor.checkForNewListings();
        verify(notifier,times(1)).send(anyString(),contains("Apartment b"));
        verify(seen,times(1)).save(any(SeenListing.class));
    }

    @Test void baselineCanBeDisabled(){
        props.setBaselineOnFirstRun(false);
        when(source.fetchListings()).thenReturn(List.of(listing("a")));
        when(seen.findAll()).thenReturn(List.of());
        when(seen.existsById("a")).thenReturn(false);
        monitor.checkForNewListings();
        verify(notifier,times(1)).send(anyString(),anyString());
    }

    @Test void failedSendIsNotMarkedSeen(){
        when(source.fetchListings()).thenReturn(List.of(listing("a")));
        when(seen.findAll()).thenReturn(List.of(new SeenListing("x")));
        when(seen.existsById("a")).thenReturn(false);
        doThrow(new IllegalStateException("twilio down")).when(notifier).send(anyString(),anyString());
        monitor.checkForNewListings();
        verify(seen,never()).save(any());
    }

    @Test void queuePositionIsAddedToAlert(){
        when(source.fetchListings()).thenReturn(List.of(listing("a")));
        when(seen.findAll()).thenReturn(List.of(new SeenListing("x")));
        when(seen.existsById("a")).thenReturn(false);
        when(queue.status()).thenReturn(QueuePositionReader.Status.VALID);
        when(queue.read(anyList())).thenReturn(Map.of("a",47));
        monitor.checkForNewListings();
        verify(notifier).send(startsWith("#47 in queue:"),contains("Your queue position: 47"));
    }

    @Test void expiredLoginIsMentionedButAlertStillSent(){
        when(source.fetchListings()).thenReturn(List.of(listing("a")));
        when(seen.findAll()).thenReturn(List.of(new SeenListing("x")));
        when(seen.existsById("a")).thenReturn(false);
        when(queue.status()).thenReturn(QueuePositionReader.Status.EXPIRED);
        monitor.checkForNewListings();
        verify(notifier).send(startsWith("New HomeQ apartment:"),contains("login expired"));
    }

    @Test void reminderMarkerDoesNotPreventFirstRunBaseline(){
        when(source.fetchListings()).thenReturn(List.of(listing("a")));
        when(seen.findAll()).thenReturn(List.of(new SeenListing("reminder:login-expiry:1")));
        monitor.checkForNewListings();
        verify(notifier,never()).send(anyString(),anyString());
        verify(seen).save(any(SeenListing.class));
    }
}
