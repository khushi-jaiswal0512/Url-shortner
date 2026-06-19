import 'url_model.dart';

class DashboardModel {
  final int totalUrls;
  final int totalClicks;
  final double averageClicksPerUrl;
  final String? mostVisitedShortCode;
  final List<UrlModel> recentUrls;
  final Map<String, int> clicksLast7Days;

  DashboardModel({
    required this.totalUrls,
    required this.totalClicks,
    required this.averageClicksPerUrl,
    this.mostVisitedShortCode,
    required this.recentUrls,
    required this.clicksLast7Days,
  });

  factory DashboardModel.fromJson(Map<String, dynamic> json) {
    var list = json['recentUrls'] as List? ?? [];
    List<UrlModel> recentUrlsList = list.map((i) => UrlModel.fromJson(i)).toList();
    
    Map<String, int> clicksLast7DaysMap = {};
    if (json['clicksLast7Days'] != null) {
      json['clicksLast7Days'].forEach((key, value) {
        clicksLast7DaysMap[key] = (value as num).toInt();
      });
    }

    return DashboardModel(
      totalUrls: json['totalUrls'] ?? 0,
      totalClicks: json['totalClicks'] ?? 0,
      averageClicksPerUrl: (json['averageClicksPerUrl'] ?? 0).toDouble(),
      mostVisitedShortCode: json['mostVisitedShortCode'],
      recentUrls: recentUrlsList,
      clicksLast7Days: clicksLast7DaysMap,
    );
  }
}
