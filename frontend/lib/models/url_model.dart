class UrlModel {
  final String shortCode;
  final String shortUrl;
  final String longUrl;
  final int clickCount;
  final DateTime? createdAt;
  final DateTime? lastAccessed;

  UrlModel({
    required this.shortCode,
    required this.shortUrl,
    required this.longUrl,
    this.clickCount = 0,
    this.createdAt,
    this.lastAccessed,
  });

  factory UrlModel.fromJson(Map<String, dynamic> json) {
    return UrlModel(
      shortCode: json['shortCode'] ?? '',
      shortUrl: json['shortUrl'] ?? '',
      longUrl: json['longUrl'] ?? '',
      clickCount: json['clickCount'] ?? 0,
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt']) : null,
      lastAccessed: json['lastAccessed'] != null ? DateTime.parse(json['lastAccessed']) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'shortCode': shortCode,
      'shortUrl': shortUrl,
      'longUrl': longUrl,
      'clickCount': clickCount,
      'createdAt': createdAt?.toIso8601String(),
      'lastAccessed': lastAccessed?.toIso8601String(),
    };
  }
}
