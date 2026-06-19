package com.urlshortener.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalUrls;
    private long totalClicks;
    private double averageClicksPerUrl;
    private String mostVisitedShortCode;
    private List<UrlResponse> recentUrls;
    private java.util.Map<String, Long> clicksLast7Days;
}
