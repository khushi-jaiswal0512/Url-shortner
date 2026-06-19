import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';

import '../providers/history_provider.dart';
import '../widgets/empty_state_widget.dart';
import '../widgets/loading_widget.dart';
import '../widgets/search_bar_widget.dart';
import '../widgets/url_card.dart';

class HistoryScreen extends ConsumerStatefulWidget {
  const HistoryScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends ConsumerState<HistoryScreen> {
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(historyProvider.notifier).fetchHistory();
    });
  }

  @override
  Widget build(BuildContext context) {
    final historyState = ref.watch(historyProvider);

    final filteredUrls = historyState.urls.where((url) {
      final query = _searchQuery.toLowerCase();
      return url.shortCode.toLowerCase().contains(query) ||
             url.longUrl.toLowerCase().contains(query);
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('History'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.read(historyProvider.notifier).fetchHistory(),
          )
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: SearchBarWidget(
              onChanged: (val) {
                setState(() {
                  _searchQuery = val;
                });
              },
            ),
          ),
          Expanded(
            child: _buildBody(historyState, filteredUrls),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(HistoryState state, List filteredUrls) {
    if (state.isLoading) {
      return const LoadingWidget();
    }
    
    if (state.error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(state.error!, style: const TextStyle(color: Colors.redAccent)),
            const Gap(16),
            ElevatedButton(
              onPressed: () => ref.read(historyProvider.notifier).fetchHistory(),
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }
    
    if (state.urls.isEmpty) {
      return const EmptyStateWidget(
        title: 'No URLs yet',
        message: 'You haven\'t shortened any URLs. Go to the Home tab to create one.',
      );
    }
    
    if (filteredUrls.isEmpty) {
      return const EmptyStateWidget(
        title: 'No matches found',
        message: 'Try adjusting your search query.',
      );
    }

    return RefreshIndicator(
      onRefresh: () => ref.read(historyProvider.notifier).fetchHistory(),
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        itemCount: filteredUrls.length,
        itemBuilder: (context, index) {
          final url = filteredUrls[index];
          return UrlCard(
            url: url,
            showDelete: true,
            onDelete: () {
              ref.read(historyProvider.notifier).deleteUrl(url.shortCode);
            },
          );
        },
      ),
    );
  }
}
