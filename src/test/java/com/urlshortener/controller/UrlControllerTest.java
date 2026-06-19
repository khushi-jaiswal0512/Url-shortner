package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.config.RateLimitFilter;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.response.UrlResponse;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller tests — tests HTTP layer in isolation.
 * Excludes RateLimitFilter to test controller logic independently.
 */
@WebMvcTest(
        controllers = UrlController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String LONG_URL = "https://www.google.com/search?q=test";
    private static final String SHORT_CODE = "aB3kPq";

    // ═══════════════════════════════════════════════
    // POST /api/v1/urls
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/urls")
    class CreateUrlTests {

        @Test
        @DisplayName("201 Created — valid new URL")
        void shouldReturn201ForNewUrl() throws Exception {
            UrlResponse response = UrlResponse.builder()
                    .shortCode(SHORT_CODE)
                    .shortUrl(BASE_URL + "/" + SHORT_CODE)
                    .longUrl(LONG_URL)
                    .createdAt(LocalDateTime.now())
                    .newlyCreated(true)
                    .build();

            when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CreateUrlRequest.builder().longUrl(LONG_URL).build())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shortCode").value(SHORT_CODE))
                    .andExpect(jsonPath("$.shortUrl").value(BASE_URL + "/" + SHORT_CODE))
                    .andExpect(jsonPath("$.longUrl").value(LONG_URL));
        }

        @Test
        @DisplayName("200 OK — duplicate URL")
        void shouldReturn200ForDuplicate() throws Exception {
            UrlResponse response = UrlResponse.builder()
                    .shortCode(SHORT_CODE)
                    .shortUrl(BASE_URL + "/" + SHORT_CODE)
                    .longUrl(LONG_URL)
                    .createdAt(LocalDateTime.now())
                    .newlyCreated(false)
                    .build();

            when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CreateUrlRequest.builder().longUrl(LONG_URL).build())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortCode").value(SHORT_CODE));
        }

        @Test
        @DisplayName("400 Bad Request — empty URL")
        void shouldReturn400ForEmptyUrl() throws Exception {
            mockMvc.perform(post("/api/v1/urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"longUrl\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — null body")
        void shouldReturn400ForNullBody() throws Exception {
            mockMvc.perform(post("/api/v1/urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — invalid URL format (no protocol)")
        void shouldReturn400ForInvalidUrlFormat() throws Exception {
            mockMvc.perform(post("/api/v1/urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"longUrl\": \"not-a-valid-url\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════
    // GET /{shortCode}
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("GET /{shortCode}")
    class RedirectTests {

        @Test
        @DisplayName("301 Moved Permanently — valid short code")
        void shouldReturn301WithLocationHeader() throws Exception {
            when(urlService.getOriginalUrl(SHORT_CODE)).thenReturn(LONG_URL);

            mockMvc.perform(get("/" + SHORT_CODE))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", LONG_URL));
        }

        @Test
        @DisplayName("404 Not Found — unknown short code")
        void shouldReturn404ForUnknownCode() throws Exception {
            when(urlService.getOriginalUrl("xyz123"))
                    .thenThrow(new UrlNotFoundException("xyz123"));

            mockMvc.perform(get("/xyz123"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // ═══════════════════════════════════════════════
    // GET /api/v1/urls/{shortCode}/analytics
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/urls/{shortCode}/analytics")
    class AnalyticsTests {

        @Test
        @DisplayName("200 OK — returns analytics data")
        void shouldReturn200WithAnalytics() throws Exception {
            UrlResponse response = UrlResponse.builder()
                    .shortCode(SHORT_CODE)
                    .shortUrl(BASE_URL + "/" + SHORT_CODE)
                    .longUrl(LONG_URL)
                    .clickCount(42L)
                    .createdAt(LocalDateTime.now())
                    .lastAccessed(LocalDateTime.now())
                    .build();

            when(urlService.getUrlAnalytics(SHORT_CODE)).thenReturn(response);

            mockMvc.perform(get("/api/v1/urls/" + SHORT_CODE + "/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clickCount").value(42))
                    .andExpect(jsonPath("$.shortCode").value(SHORT_CODE));
        }

        @Test
        @DisplayName("404 Not Found — analytics for unknown code")
        void shouldReturn404ForUnknownAnalytics() throws Exception {
            when(urlService.getUrlAnalytics("xyz"))
                    .thenThrow(new UrlNotFoundException("xyz"));

            mockMvc.perform(get("/api/v1/urls/xyz/analytics"))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════
    // DELETE /api/v1/urls/{shortCode}
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/urls/{shortCode}")
    class DeleteTests {

        @Test
        @DisplayName("204 No Content — successful deletion")
        void shouldReturn204OnDeletion() throws Exception {
            doNothing().when(urlService).deleteUrl(SHORT_CODE);

            mockMvc.perform(delete("/api/v1/urls/" + SHORT_CODE))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 Not Found — delete unknown code")
        void shouldReturn404OnDeleteUnknown() throws Exception {
            doThrow(new UrlNotFoundException("xyz"))
                    .when(urlService).deleteUrl("xyz");

            mockMvc.perform(delete("/api/v1/urls/xyz"))
                    .andExpect(status().isNotFound());
        }
    }
}
