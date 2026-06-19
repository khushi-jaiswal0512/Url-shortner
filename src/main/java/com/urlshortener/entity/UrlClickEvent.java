package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_click_events", indexes = {
        @Index(name = "idx_click_url_id", columnList = "url_id"),
        @Index(name = "idx_click_date", columnList = "clicked_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private UrlEntity url;

    @CreationTimestamp
    @Column(name = "clicked_at", updatable = false)
    private LocalDateTime clickedAt;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "referrer", length = 512)
    private String referrer;
}
