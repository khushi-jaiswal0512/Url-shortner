import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/api_constants.dart';

final sharedPreferencesProvider = Provider<SharedPreferences>((ref) {
  throw UnimplementedError();
});

class SettingsState {
  final bool isDarkMode;
  final String baseUrl;

  SettingsState({
    required this.isDarkMode,
    required this.baseUrl,
  });

  SettingsState copyWith({
    bool? isDarkMode,
    String? baseUrl,
  }) {
    return SettingsState(
      isDarkMode: isDarkMode ?? this.isDarkMode,
      baseUrl: baseUrl ?? this.baseUrl,
    );
  }
}

class SettingsNotifier extends Notifier<SettingsState> {
  @override
  SettingsState build() {
    final prefs = ref.watch(sharedPreferencesProvider);
    return SettingsState(
      isDarkMode: prefs.getBool('isDarkMode') ?? true,
      baseUrl: prefs.getString('baseUrl') ?? ApiConstants.baseUrl,
    );
  }

  void toggleTheme() {
    final prefs = ref.read(sharedPreferencesProvider);
    final newValue = !state.isDarkMode;
    prefs.setBool('isDarkMode', newValue);
    state = state.copyWith(isDarkMode: newValue);
  }

  void updateBaseUrl(String newUrl) {
    final prefs = ref.read(sharedPreferencesProvider);
    prefs.setString('baseUrl', newUrl);
    state = state.copyWith(baseUrl: newUrl);
  }
}

final settingsProvider = NotifierProvider<SettingsNotifier, SettingsState>(() {
  return SettingsNotifier();
});
