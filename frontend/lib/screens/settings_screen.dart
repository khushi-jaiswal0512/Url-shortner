import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/custom_button.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  late TextEditingController _baseUrlController;

  @override
  void initState() {
    super.initState();
    final currentUrl = ref.read(settingsProvider).baseUrl;
    _baseUrlController = TextEditingController(text: currentUrl);
  }

  @override
  void dispose() {
    _baseUrlController.dispose();
    super.dispose();
  }

  void _saveSettings() {
    final newUrl = _baseUrlController.text.trim();
    if (newUrl.isNotEmpty) {
      ref.read(settingsProvider.notifier).updateBaseUrl(newUrl);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Settings saved successfully.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Backend Configuration',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const Gap(16),
            const Text(
              'Base URL',
              style: TextStyle(color: AppTheme.textGrey, fontSize: 14),
            ),
            const Gap(8),
            TextField(
              controller: _baseUrlController,
              decoration: const InputDecoration(
                hintText: 'http://localhost:8080',
                prefixIcon: Icon(LucideIcons.link),
              ),
            ),
            const Gap(8),
            const Text(
              'Change this to your deployed backend URL (e.g. https://api.my-url-shortener.com)',
              style: TextStyle(color: AppTheme.textGrey, fontSize: 12),
            ),
            const Gap(32),
            CustomButton(text: 'Save Settings', onPressed: _saveSettings),
            const Gap(48),
            const Center(
              child: Text(
                'Version 1.0.0',
                style: TextStyle(color: AppTheme.textGrey, fontSize: 14),
              ),
            )
          ],
        ),
      ),
    );
  }
}
