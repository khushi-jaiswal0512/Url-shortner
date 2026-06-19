import 
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/url_model.dart';
import 'service_providers.dart';
import 'history_provider.dart';
import 'dashboard_provider.dart';

class UrlState {
  final bool isLoading;
  final String? error;
  final UrlModel? urlResult;

  UrlState({this.isLoading = false, this.error, this.urlResult});

  UrlState copyWith({bool? isLoading, String? error, UrlModel? urlResult, bool clearError = false}) {
    return UrlState(
      isLoading: isLoading ?? this.isLoading,
      error: clearError ? null : (error ?? this.error),
      urlResult: urlResult ?? this.urlResult,
    );
  }
}

class UrlNotifier extends Notifier<UrlState> {
  @override
  UrlState build() {
    return UrlState();
  }

  Future<void> shortenUrl(String longUrl, {String? customAlias, DateTime? expiresAt}) async {
    state = state.copyWith(isLoading: true, clearError: true);
    final service = ref.read(urlServiceProvider);
    
    final response = await service.createShortUrl(longUrl, customAlias: customAlias, expiresAt: expiresAt);
    
    if (response.isSuccess && response.data != null) {
      state = state.copyWith(isLoading: false, urlResult: response.data);
      // Refresh dashboard and history
      ref.invalidate(dashboardProvider);
      ref.invalidate(historyProvider);
    } else {
      state = state.copyWith(isLoading: false, error: response.errorMessage ?? 'Failed to shorten URL');
    }
  }

  void clearResult() {
    state = UrlState();
  }
}

final urlProvider = NotifierProvider<UrlNotifier, UrlState>(() {
  return UrlNotifier();
});

// Since history and dashboard providers are needed here, we declare their providers below:

