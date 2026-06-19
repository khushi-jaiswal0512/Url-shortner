import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/dashboard_model.dart';
import 'service_providers.dart';

class DashboardState {
  final bool isLoading;
  final String? error;
  final DashboardModel? dashboardData;

  DashboardState({this.isLoading = true, this.error, this.dashboardData});

  DashboardState copyWith({bool? isLoading, String? error, DashboardModel? dashboardData, bool clearError = false}) {
    return DashboardState(
      isLoading: isLoading ?? this.isLoading,
      error: clearError ? null : (error ?? this.error),
      dashboardData: dashboardData ?? this.dashboardData,
    );
  }
}

class DashboardNotifier extends Notifier<DashboardState> {
  @override
  DashboardState build() {
    Future.microtask(() => fetchDashboard());
    return DashboardState();
  }

  Future<void> fetchDashboard() async {
    state = state.copyWith(isLoading: true, clearError: true);
    final service = ref.read(dashboardServiceProvider);
    
    final response = await service.getDashboardStats();
    
    if (response.isSuccess && response.data != null) {
      state = state.copyWith(isLoading: false, dashboardData: response.data);
    } else {
      state = state.copyWith(isLoading: false, error: response.errorMessage ?? 'Failed to load dashboard');
    }
  }
}

final dashboardProvider = NotifierProvider<DashboardNotifier, DashboardState>(() {
  return DashboardNotifier();
});
