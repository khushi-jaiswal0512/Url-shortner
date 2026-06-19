import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/api_service.dart';
import '../services/url_service.dart';
import '../services/history_service.dart';
import '../services/dashboard_service.dart';
import 'settings_provider.dart';

final apiServiceProvider = Provider<ApiService>((ref) {
  final baseUrl = ref.watch(settingsProvider).baseUrl;
  return ApiService(baseUrl);
});

final urlServiceProvider = Provider<UrlService>((ref) {
  return UrlService(ref.watch(apiServiceProvider));
});

final historyServiceProvider = Provider<HistoryService>((ref) {
  return HistoryService(ref.watch(apiServiceProvider));
});

final dashboardServiceProvider = Provider<DashboardService>((ref) {
  return DashboardService(ref.watch(apiServiceProvider));
});
