import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';
import 'package:fl_chart/fl_chart.dart' as fl_chart;

import '../providers/dashboard_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/empty_state_widget.dart';
import '../widgets/loading_widget.dart';
import '../widgets/url_card.dart';

class DashboardScreen extends ConsumerStatefulWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends ConsumerState<DashboardScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(dashboardProvider.notifier).fetchDashboard();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(dashboardProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Dashboard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.read(dashboardProvider.notifier).fetchDashboard(),
          )
        ],
      ),
      body: _buildBody(state),
    );
  }

  Widget _buildBody(DashboardState state) {
    if (state.isLoading) return const LoadingWidget();

    if (state.error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(state.error!, style: const TextStyle(color: Colors.redAccent)),
            const Gap(16),
            ElevatedButton(
              onPressed: () => ref.read(dashboardProvider.notifier).fetchDashboard(),
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    final data = state.dashboardData;
    if (data == null) return const EmptyStateWidget(title: 'No Data', message: 'No dashboard data available.');

    return RefreshIndicator(
      onRefresh: () => ref.read(dashboardProvider.notifier).fetchDashboard(),
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
              Row(
                children: [
                  Expanded(child: _StatCard(title: 'Total URLs', value: data.totalUrls.toString())),
                  const Gap(16),
                  Expanded(child: _StatCard(title: 'Total Clicks', value: data.totalClicks.toString())),
                ],
              ),
              const Gap(16),
              Row(
                children: [
                  Expanded(child: _StatCard(title: 'Avg Clicks/URL', value: data.averageClicksPerUrl.toStringAsFixed(1))),
                  const Gap(16),
                  if (data.mostVisitedShortCode != null)
                    Expanded(
                      child: Card(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 20.0, horizontal: 16.0),
                          child: Column(
                            children: [
                              const Text('Most Visited', style: TextStyle(color: AppTheme.textGrey, fontSize: 14)),
                              const Gap(8),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(Icons.star, color: Colors.amber, size: 16),
                                  const Gap(4),
                                  Text(
                                    data.mostVisitedShortCode!,
                                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.primaryColor),
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    )
                  else
                    const Expanded(child: SizedBox()),
                ],
              ),
              if (data.clicksLast7Days.isNotEmpty) ...[
                const Gap(32),
                const Text('Clicks Last 7 Days', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                const Gap(16),
                SizedBox(
                  height: 250,
                  child: Card(
                    padding: const EdgeInsets.all(16),
                    child: Padding(
                      padding: const EdgeInsets.only(top: 24.0, right: 16.0),
                      child: fl_chart.BarChart(
                        fl_chart.BarChartData(
                          alignment: fl_chart.BarChartAlignment.spaceAround,
                          maxY: data.clicksLast7Days.values.fold(0, (max, e) => e > max ? e : max).toDouble() * 1.2,
                          barTouchData: fl_chart.BarTouchData(enabled: false),
                          titlesData: fl_chart.FlTitlesData(
                            show: true,
                            bottomTitles: fl_chart.AxisTitles(
                              sideTitles: fl_chart.SideTitles(
                                showTitles: true,
                                getTitlesWidget: (value, meta) {
                                  final index = value.toInt();
                                  if (index >= 0 && index < data.clicksLast7Days.keys.length) {
                                    final dateStr = data.clicksLast7Days.keys.elementAt(index);
                                    // Parse or just show last 5 chars (MM-DD)
                                    final display = dateStr.length >= 10 ? dateStr.substring(5) : dateStr;
                                    return Padding(
                                      padding: const EdgeInsets.only(top: 8.0),
                                      child: Text(display, style: const TextStyle(color: AppTheme.textGrey, fontSize: 10)),
                                    );
                                  }
                                  return const Text('');
                                },
                              ),
                            ),
                            leftTitles: fl_chart.AxisTitles(
                              sideTitles: fl_chart.SideTitles(
                                showTitles: true,
                                reservedSize: 28,
                                getTitlesWidget: (value, meta) {
                                  if (value == value.toInt()) {
                                    return Text(value.toInt().toString(), style: const TextStyle(color: AppTheme.textGrey, fontSize: 10));
                                  }
                                  return const Text('');
                                },
                              ),
                            ),
                            topTitles: const fl_chart.AxisTitles(sideTitles: fl_chart.SideTitles(showTitles: false)),
                            rightTitles: const fl_chart.AxisTitles(sideTitles: fl_chart.SideTitles(showTitles: false)),
                          ),
                          gridData: fl_chart.FlGridData(
                            show: true,
                            drawVerticalLine: false,
                            getDrawingHorizontalLine: (value) => fl_chart.FlLine(color: Colors.grey.withOpacity(0.2), strokeWidth: 1),
                          ),
                          borderData: fl_chart.FlBorderData(show: false),
                          barGroups: List.generate(
                            data.clicksLast7Days.length,
                            (index) => fl_chart.BarChartGroupData(
                              x: index,
                              barRods: [
                                fl_chart.BarChartRodData(
                                  toY: data.clicksLast7Days.values.elementAt(index).toDouble(),
                                  color: AppTheme.primaryColor,
                                  width: 16,
                                  borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
              const Gap(32),
            const Text('Recent URLs', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const Gap(16),
            if (data.recentUrls.isEmpty)
              const Text('No recent URLs found.', style: TextStyle(color: AppTheme.textGrey))
            else
              ...data.recentUrls.map((url) => UrlCard(url: url)).toList(),
          ],
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String title;
  final String value;

  const _StatCard({required this.title, required this.value});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            Text(title, style: const TextStyle(color: AppTheme.textGrey, fontSize: 14)),
            const Gap(8),
            Text(value, style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: AppTheme.primaryColor)),
          ],
        ),
      ),
    );
  }
}
