import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';

import '../providers/url_provider.dart';
import '../widgets/custom_button.dart';
import '../widgets/url_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final TextEditingController _urlController = TextEditingController();
  final TextEditingController _aliasController = TextEditingController();
  DateTime? _selectedDate;
  bool _showAdvanced = false;

  void _shorten() {
    final url = _urlController.text.trim();
    if (url.isEmpty) return;

    final alias = _aliasController.text.trim();

    String formattedUrl = url;
    if (!formattedUrl.startsWith('http://') && !formattedUrl.startsWith('https://')) {
      formattedUrl = 'https://$formattedUrl';
    }
    
    ref.read(urlProvider.notifier).shortenUrl(
      formattedUrl, 
      customAlias: alias.isNotEmpty ? alias : null,
      expiresAt: _selectedDate,
    );
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now().add(const Duration(days: 7)),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 5)),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
      });
    }
  }

  @override
  void dispose() {
    _urlController.dispose();
    _aliasController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final urlState = ref.watch(urlProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('URL Shortener'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Gap(20),
            const Text(
              'Shorten your long links',
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
            const Gap(8),
            const Text(
              'Paste your long URL below to create a concise, shareable link.',
              style: TextStyle(fontSize: 16, color: Colors.grey),
              textAlign: TextAlign.center,
            ),
            const Gap(40),
            TextField(
              controller: _urlController,
              decoration: const InputDecoration(
                hintText: 'https://example.com/very/long/path',
                prefixIcon: Icon(Icons.link),
              ),
              onSubmitted: (_) => _shorten(),
            ),
            const Gap(16),
            InkWell(
              onTap: () {
                setState(() {
                  _showAdvanced = !_showAdvanced;
                });
              },
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      _showAdvanced ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                      color: Theme.of(context).primaryColor,
                    ),
                    const Gap(8),
                    Text(
                      'Advanced Options',
                      style: TextStyle(
                        color: Theme.of(context).primaryColor,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            if (_showAdvanced) ...[
              const Gap(16),
              TextField(
                controller: _aliasController,
                decoration: const InputDecoration(
                  labelText: 'Custom Alias (Optional)',
                  hintText: 'e.g. my-portfolio',
                  prefixIcon: Icon(Icons.short_text),
                ),
              ),
              const Gap(16),
              InkWell(
                onTap: () => _selectDate(context),
                child: InputDecorator(
                  decoration: const InputDecoration(
                    labelText: 'Expiration Date (Optional)',
                    prefixIcon: Icon(Icons.calendar_today),
                  ),
                  child: Text(
                    _selectedDate == null 
                        ? 'Never expire' 
                        : '${_selectedDate!.year}-${_selectedDate!.month.toString().padLeft(2, '0')}-${_selectedDate!.day.toString().padLeft(2, '0')}',
                  ),
                ),
              ),
            ],
            const Gap(24),
            CustomButton(
              text: 'Shorten URL',
              onPressed: _shorten,
              isLoading: urlState.isLoading,
            ),
            const Gap(40),
            if (urlState.error != null)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.red.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: Colors.red.withOpacity(0.3)),
                ),
                child: Text(
                  urlState.error!,
                  style: const TextStyle(color: Colors.redAccent),
                  textAlign: TextAlign.center,
                ),
              ),
            if (urlState.urlResult != null) ...[
              const Text(
                'Your Shortened URL',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const Gap(16),
              UrlCard(url: urlState.urlResult!),
            ]
          ],
        ),
      ),
    );
  }
}
