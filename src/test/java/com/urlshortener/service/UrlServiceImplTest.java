package com.urlshortener.service;

import com.urlshortener.cache.UrlCacheService;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.entity.UrlEntity;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.response.UrlResponse;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;
    @Mock
    private Base62Encoder base62Encoder;
    @Mock
    private UrlCacheService cacheService;
    @Mock
    private UrlMetricsService metricsService;

    @InjectMocks
    private UrlServiceImpl urlService;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String LONG_URL = "https://www.google.com/search?q=system+design";
    private static final String SHORT_CODE = "aB3kPq";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", BASE_URL);
    }

    // ═══════════════════════════════════════════════
    // createShortUrl tests
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("createShortUrl")
    class CreateShortUrlTests {

        @Test
        @DisplayName("Should create new short URL for valid long URL")
        void shouldCreateNewShortUrl() {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .longUrl(LONG_URL)
                    .build();

            UrlEntity savedEntity = UrlEntity.builder()
                    .id(1L)
                    .longUrl(LONG_URL)
                    .urlHash("somehash")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(urlRepository.findByUrlHashAndIsActiveTrue(anyString()))
                    .thenReturn(Optional.empty());
            when(urlRepository.saveAndFlush(any(UrlEntity.class)))
                    .thenReturn(savedEntity);
            when(base62Encoder.encode(1L)).thenReturn(SHORT_CODE);
            when(urlRepository.save(any(UrlEntity.class)))
                    .thenReturn(savedEntity);

            UrlResponse response = urlService.createShortUrl(request);

            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getShortUrl()).isEqualTo(BASE_URL + "/" + SHORT_CODE);
            assertThat(response.getLongUrl()).isEqualTo(LONG_URL);
            assertThat(response.isNewlyCreated()).isTrue();

            verify(urlRepository).saveAndFlush(any(UrlEntity.class));
            verify(cacheService).cacheUrl(SHORT_CODE, LONG_URL);
        }

        @Test
        @DisplayName("Should return existing short URL for duplicate long URL")
        void shouldReturnExistingForDuplicate() {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .longUrl(LONG_URL)
                    .build();

            UrlEntity existingEntity = UrlEntity.builder()
                    .id(1L)
                    .shortCode(SHORT_CODE)
                    .longUrl(LONG_URL)
                    .urlHash("somehash")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(urlRepository.findByUrlHashAndIsActiveTrue(anyString()))
                    .thenReturn(Optional.of(existingEntity));

            UrlResponse response = urlService.createShortUrl(request);

            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.isNewlyCreated()).isFalse();

            verify(urlRepository, never()).saveAndFlush(any());
            verify(cacheService, never()).cacheUrl(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw InvalidUrlException for circular redirect")
        void shouldThrowForCircularRedirect() {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .longUrl("http://localhost:8080/someCode")
                    .build();

            assertThatThrownBy(() -> urlService.createShortUrl(request))
                    .isInstanceOf(InvalidUrlException.class)
                    .hasMessageContaining("circular");
        }
    }

    // ═══════════════════════════════════════════════
    // getOriginalUrl tests
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("getOriginalUrl")
    class GetOriginalUrlTests {

        @Test
        @DisplayName("Should return cached URL on cache hit")
        void shouldReturnCachedUrl() {
            when(cacheService.getCachedUrl(SHORT_CODE)).thenReturn(LONG_URL);

            String result = urlService.getOriginalUrl(SHORT_CODE);

            assertThat(result).isEqualTo(LONG_URL);
            verify(metricsService).incrementClickAsync(SHORT_CODE);
            verify(urlRepository, never()).findByShortCodeAndIsActiveTrue(anyString());
        }

        @Test
        @DisplayName("Should fetch from DB and populate cache on cache miss")
        void shouldFetchFromDbOnCacheMiss() {
            UrlEntity entity = UrlEntity.builder()
                    .shortCode(SHORT_CODE)
                    .longUrl(LONG_URL)
                    .build();

            when(cacheService.getCachedUrl(SHORT_CODE)).thenReturn(null);
            when(urlRepository.findByShortCodeAndIsActiveTrue(SHORT_CODE))
                    .thenReturn(Optional.of(entity));

            String result = urlService.getOriginalUrl(SHORT_CODE);

            assertThat(result).isEqualTo(LONG_URL);
            verify(cacheService).cacheUrl(SHORT_CODE, LONG_URL);
            verify(metricsService).incrementClickAsync(SHORT_CODE);
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException for unknown short code")
        void shouldThrowForUnknownShortCode() {
            when(cacheService.getCachedUrl(SHORT_CODE)).thenReturn(null);
            when(urlRepository.findByShortCodeAndIsActiveTrue(SHORT_CODE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.getOriginalUrl(SHORT_CODE))
                    .isInstanceOf(UrlNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════
    // getUrlAnalytics tests
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("getUrlAnalytics")
    class GetUrlAnalyticsTests {

        @Test
        @DisplayName("Should return analytics for active short code")
        void shouldReturnAnalytics() {
            LocalDateTime now = LocalDateTime.now();
            UrlEntity entity = UrlEntity.builder()
                    .shortCode(SHORT_CODE)
                    .longUrl(LONG_URL)
                    .clickCount(42L)
                    .createdAt(now)
                    .lastAccessed(now)
                    .build();

            when(urlRepository.findByShortCodeAndIsActiveTrue(SHORT_CODE))
                    .thenReturn(Optional.of(entity));

            UrlResponse response = urlService.getUrlAnalytics(SHORT_CODE);

            assertThat(response.getClickCount()).isEqualTo(42L);
            assertThat(response.getLastAccessed()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException for missing code")
        void shouldThrowForMissingCode() {
            when(urlRepository.findByShortCodeAndIsActiveTrue("xyz"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.getUrlAnalytics("xyz"))
                    .isInstanceOf(UrlNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════
    // deleteUrl tests
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("deleteUrl")
    class DeleteUrlTests {

        @Test
        @DisplayName("Should soft-delete and evict cache")
        void shouldSoftDeleteAndEvictCache() {
            UrlEntity entity = UrlEntity.builder()
                    .shortCode(SHORT_CODE)
                    .longUrl(LONG_URL)
                    .isActive(true)
                    .build();

            when(urlRepository.findByShortCodeAndIsActiveTrue(SHORT_CODE))
                    .thenReturn(Optional.of(entity));

            urlService.deleteUrl(SHORT_CODE);

            assertThat(entity.getIsActive()).isFalse();
            verify(urlRepository).save(entity);
            verify(cacheService).evictUrl(SHORT_CODE);
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException for already deleted code")
        void shouldThrowForAlreadyDeletedCode() {
            when(urlRepository.findByShortCodeAndIsActiveTrue(SHORT_CODE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.deleteUrl(SHORT_CODE))
                    .isInstanceOf(UrlNotFoundException.class);
        }
    }
}
