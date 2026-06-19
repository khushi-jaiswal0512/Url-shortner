package com.urlshortener.controller;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.response.ErrorResponse;
import com.urlshortener.response.UrlResponse;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for URL shortening operations.
 * <p>
 * Endpoints:
 * - POST   /api/v1/urls                    → Create short URL
 * - GET    /{shortCode}                     → 301 Redirect
 * - GET    /api/v1/urls/{shortCode}/analytics → Fetch analytics
 * - DELETE /api/v1/urls/{shortCode}         → Soft delete
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "APIs for URL shortening, redirection, analytics, and deletion")
public class UrlController {

    private final UrlService urlService;

    // ─── POST /api/v1/urls ───────────────────────────────────────

    @Operation(summary = "Create Short URL",
            description = "Accepts a long URL and returns a unique short URL. "
                    + "If the URL was previously shortened, returns the existing mapping.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created",
                    content = @Content(schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "200", description = "Duplicate — existing short URL returned",
                    content = @Content(schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid URL",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request) {

        log.info("POST /api/v1/urls — longUrl: {}", request.getLongUrl());
        UrlResponse response = urlService.createShortUrl(request);

        HttpStatus status = response.isNewlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    // ─── GET /{shortCode} ────────────────────────────────────────

    @Operation(summary = "Redirect to Original URL",
            description = "Resolves the short code and redirects to the original long URL using HTTP 301.")
    @ApiResponses({
            @ApiResponse(responseCode = "301", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short code not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code to resolve", example = "aB3kPq")
            @PathVariable String shortCode) {

        log.info("GET /{} — resolving redirect", shortCode);
        String longUrl = urlService.getOriginalUrl(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    // ─── GET /api/v1/urls/{shortCode}/analytics ──────────────────

    @Operation(summary = "Get URL Analytics",
            description = "Returns detailed analytics for a shortened URL including click count and timestamps.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics data",
                    content = @Content(schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "404", description = "Short code not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/urls/{shortCode}/analytics")
    public ResponseEntity<UrlResponse> getAnalytics(
            @Parameter(description = "The short code to look up", example = "aB3kPq")
            @PathVariable String shortCode) {

        log.info("GET /api/v1/urls/{}/analytics", shortCode);
        UrlResponse response = urlService.getUrlAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

    // ─── GET /api/v1/urls ────────────────────────────────────────

    @Operation(summary = "Get All URLs",
            description = "Returns a list of all active shortened URLs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of URLs")
    })
    @GetMapping("/api/v1/urls")
    public ResponseEntity<java.util.List<UrlResponse>> getAllUrls() {
        log.info("GET /api/v1/urls");
        return ResponseEntity.ok(urlService.getAllUrls());
    }

    // ─── GET /api/v1/dashboard ───────────────────────────────────

    @Operation(summary = "Get Dashboard Stats",
            description = "Returns aggregated statistics and recently shortened URLs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard stats")
    })
    @GetMapping("/api/v1/dashboard")
    public ResponseEntity<com.urlshortener.response.DashboardResponse> getDashboardStats() {
        log.info("GET /api/v1/dashboard");
        return ResponseEntity.ok(urlService.getDashboardStats());
    }

    @Operation(summary = "Delete Short URL",
            description = "Soft-deletes the URL mapping and evicts it from the Redis cache.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "URL deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Short code not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @Parameter(description = "The short code to delete", example = "aB3kPq")
            @PathVariable String shortCode) {

        log.info("DELETE /api/v1/urls/{}", shortCode);
        urlService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }
}
