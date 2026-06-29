package com.randmteam2.tripplanning.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCacheInvalidationServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Test
    void evictBookingReadCachesScansAndDeletesAllReadCachePatterns() {
        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenAnswer(invocation -> new ListCursor(List.of(
                        invocation.<ScanOptions>getArgument(0).getPattern().replace("*", "1")
                )));

        BookingCacheInvalidationService service = new BookingCacheInvalidationService(redisTemplate);
        service.evictBookingReadCaches();

        assertScannedPatterns(
                "booking-service::booking::*",
                "booking-service::S5-F1::*",
                "booking-service::S5-F3::*",
                "booking-service::S5-F6::*",
                "booking-service::S5-F8::*",
                "booking-service::S5-F9::*",
                "booking-service::S5-F10::*",
                "booking-service::S5-F11::*",
                "booking-service::S5-SYNC-CONFIRMED-SUMMARY::*",
                "booking-service::S5-SYNC-USER-TOTAL::*",
                "booking-service::S5-SYNC-AGGREGATE::*"
        );

        ArgumentCaptor<Collection<String>> deleteCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate, org.mockito.Mockito.times(11)).delete(deleteCaptor.capture());
        assertThat(deleteCaptor.getAllValues())
                .allSatisfy(keys -> assertThat(keys).hasSize(1));
    }

    @Test
    void evictRefundRelatedCachesIncludesAnalyticsPaymentHistoryAndBookingPatterns() {
        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenAnswer(invocation -> new ListCursor(List.of(
                        invocation.<ScanOptions>getArgument(0).getPattern().replace("*", "1")
                )));

        BookingCacheInvalidationService service = new BookingCacheInvalidationService(redisTemplate);
        service.evictRefundRelatedCaches();

        assertScannedPatterns(
                "booking-service::booking::*",
                "booking-service::S5-F1::*",
                "booking-service::S5-F3::*",
                "booking-service::S5-F6::*",
                "booking-service::S5-F8::*",
                "booking-service::S5-F9::*",
                "booking-service::S5-F10::*",
                "booking-service::S5-F11::*",
                "booking-service::S5-SYNC-CONFIRMED-SUMMARY::*",
                "booking-service::S5-SYNC-USER-TOTAL::*",
                "booking-service::S5-SYNC-AGGREGATE::*"
        );

        verify(redisTemplate, org.mockito.Mockito.times(11)).delete(any(Collection.class));
    }

    @Test
    void doesNotCallDeleteWhenScanFindsNoKeys() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(new ListCursor(List.of()));

        BookingCacheInvalidationService service = new BookingCacheInvalidationService(redisTemplate);
        service.evictBookingReadCaches();

        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    private void assertScannedPatterns(String... expectedPatterns) {
        ArgumentCaptor<ScanOptions> optionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redisTemplate, org.mockito.Mockito.times(expectedPatterns.length)).scan(optionsCaptor.capture());

        List<String> actualPatterns = optionsCaptor.getAllValues().stream()
                .map(ScanOptions::getPattern)
                .toList();

        assertThat(actualPatterns).containsExactly(expectedPatterns);
        assertThat(optionsCaptor.getAllValues())
                .allSatisfy(options -> assertThat(options.getCount()).isEqualTo(1000));
    }

    private static class ListCursor implements Cursor<String> {
        private final Iterator<String> iterator;
        private long position;
        private boolean closed;

        private ListCursor(List<String> values) {
            this.iterator = new ArrayList<>(values).iterator();
        }

        @Override
        public CursorId getId() {
            return CursorId.of(0);
        }

        @Override
        public long getCursorId() {
            return 0;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            position++;
            return iterator.next();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
