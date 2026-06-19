package com.urlshortener.repository;

import com.urlshortener.entity.UrlEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer tests — uses H2 in-memory database via @DataJpaTest.
 */
@DataJpaTest
@ActiveProfiles("test")
class UrlRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    private UrlEntity savedEntity;

    @BeforeEach
    void setUp() {
        urlRepository.deleteAll();

        UrlEntity entity = UrlEntity.builder()
                .shortCode("abc123")
                .urlHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .longUrl("https://www.example.com/very/long/url")
                .clickCount(0L)
                .isActive(true)
                .build();

        savedEntity = urlRepository.saveAndFlush(entity);
    }

    @Test
    @DisplayName("Should save entity and auto-generate ID")
    void shouldSaveAndGenerateId() {
        assertThat(savedEntity.getId()).isNotNull();
        assertThat(savedEntity.getId()).isPositive();
    }

    @Test
    @DisplayName("Should find active entity by short code")
    void shouldFindByShortCodeAndActive() {
        Optional<UrlEntity> found = urlRepository.findByShortCodeAndIsActiveTrue("abc123");

        assertThat(found).isPresent();
        assertThat(found.get().getLongUrl()).isEqualTo("https://www.example.com/very/long/url");
    }

    @Test
    @DisplayName("Should not find inactive entity by short code")
    void shouldNotFindInactiveByShortCode() {
        savedEntity.setIsActive(false);
        urlRepository.save(savedEntity);

        Optional<UrlEntity> found = urlRepository.findByShortCodeAndIsActiveTrue("abc123");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find active entity by url hash")
    void shouldFindByUrlHash() {
        Optional<UrlEntity> found = urlRepository.findByUrlHashAndIsActiveTrue(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        assertThat(found).isPresent();
        assertThat(found.get().getShortCode()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("Should return empty for non-existent hash")
    void shouldReturnEmptyForNonExistentHash() {
        Optional<UrlEntity> found = urlRepository.findByUrlHashAndIsActiveTrue("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find by short code regardless of active status")
    void shouldFindByShortCodeRegardless() {
        savedEntity.setIsActive(false);
        urlRepository.save(savedEntity);

        Optional<UrlEntity> found = urlRepository.findByShortCode("abc123");
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should save entity with null short code (two-phase insert)")
    void shouldSaveWithNullShortCode() {
        UrlEntity entity = UrlEntity.builder()
                .urlHash("a1b2c3d4e5f6")
                .longUrl("https://example.com/test")
                .build();

        UrlEntity saved = urlRepository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getShortCode()).isNull();

        // Phase 2: update with short code
        saved.setShortCode("xyz789");
        urlRepository.save(saved);

        Optional<UrlEntity> found = urlRepository.findByShortCodeAndIsActiveTrue("xyz789");
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Entity roundtrip — all fields preserved")
    void entityRoundtrip() {
        Optional<UrlEntity> found = urlRepository.findById(savedEntity.getId());

        assertThat(found).isPresent();
        UrlEntity entity = found.get();
        assertThat(entity.getShortCode()).isEqualTo("abc123");
        assertThat(entity.getLongUrl()).isEqualTo("https://www.example.com/very/long/url");
        assertThat(entity.getClickCount()).isEqualTo(0L);
        assertThat(entity.getIsActive()).isTrue();
        assertThat(entity.getCreatedAt()).isNotNull();
    }
}
