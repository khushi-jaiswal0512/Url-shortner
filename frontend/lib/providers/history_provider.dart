import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/url_model.dart';
import 'service_providers.dart';
import 'dashboard_provider.dart';
import 'url_provider.dart';

class HistoryState {
  final bool isLoading;
  final String? error;
  final List<UrlModel> urls;

  HistoryState({this.isLoading = true, this.error, this.urls = const []});

  HistoryState copyWith({bool? isLoading, String? error, List<UrlModel>? urls, bool clearError = false}) {
    return HistoryState(
      isLoading: isLoading ?? this.isLoading,
      error: clearError ? null : (error ?? this.error),
      urls: urls ?? this.urls,
    );
  }
}

class HistoryNotifier extends Notifier<HistoryState> {
  @override
  HistoryState build() {
    // Cannot call async methods directly in build that update state immediately before build returns,
    // so we delay the initial fetch slightly or just return initial state.
    Future.microtask(() => fetchHistory());
    return HistoryState();
  }

  Future<void> fetchHistory() async {
    state = state.copyWith(isLoading: true, clearError: true);
    final service = ref.read(historyServiceProvider);
    
    final response = await service.getAllUrls();
    
    if (response.isSuccess && response.data != null) {
      state = state.copyWith(isLoading: false, urls: response.data!);
    } else {
      state = state.copyWith(isLoading: false, error: response.errorMessage ?? 'Failed to fetch history');
    }
  }

  Future<bool> deleteUrl(String shortCode) async {
    final service = ref.read(urlServiceProvider);
    final response = await service.deleteUrl(shortCode);
    if (response.isSuccess) {
      // Remove locally to avoid full refetch
      final newUrls = state.urls.where((u) => u.shortCode != shortCode).toList();
      state = state.copyWith(urls: newUrls);
      
      // We also need to refresh dashboard stats
      ref.invalidate(dashboardProvider);
      return true;
    }
    return false;
  }
}

final historyProvider = NotifierProvider<HistoryNotifier, HistoryState>(() {
  return HistoryNotifier();
});
