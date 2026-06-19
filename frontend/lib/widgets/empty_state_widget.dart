import 'package:flutter/material.dart';
import 'package:gap/gap.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../theme/app_theme.dart';

class EmptyStateWidget extends StatelessWidget {
  final String title;
  final String message;
  final IconData icon;

  const EmptyStateWidget({
    Key? key,
    required this.title,
    required this.message,
    this.icon = LucideIcons.inbox,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 64, color: AppTheme.border),
          const Gap(16),
          Text(
            title,
            style: const TextStyle(
              color: AppTheme.textWhite,
              fontSize: 18,
              fontWeight: FontWeight.w600,
            ),
          ),
          const Gap(8),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: AppTheme.textGrey,
              fontSize: 14,
            ),
          ),
        ],
      ),
    );
  }
}
