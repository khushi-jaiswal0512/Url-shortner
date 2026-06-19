class ApiConstants {
  // Use 10.0.2.2 for Android Emulator connecting to localhost
  // Use localhost for Web
  // Actually, we will read this from SettingsProvider, but this is the default fallback.
  static String baseUrl = 'http://localhost:8080';

  // Endpoints
  static const String createUrl = '/api/v1/urls';
  static const String getUrls = '/api/v1/urls';
  static const String getDashboard = '/api/v1/dashboard';
  
  static String deleteUrl(String shortCode) => '/api/v1/urls/$shortCode';
  static String getAnalytics(String shortCode) => '/api/v1/urls/$shortCode/analytics';
}
