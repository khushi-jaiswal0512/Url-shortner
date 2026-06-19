import 'package:flutter/material.dart';
import 'package:gap/gap.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/url_model.dart';
import '../theme/app_theme.dart';
import 'copy_button_widget.dart';
import 'qr_code_modal.dart';

class UrlCard extends StatelessWidget {
  final UrlModel url;
  final VoidCallback? onDelete;
  final bool showDelete;

  const UrlCard({
    Key? key,
    required this.url,
    this.onDelete,
    this.showDelete = false,
  }) : super(key: key);

  void _openUrl() async {
    final uri = Uri.parse(url.shortUrl);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    url.shortUrl,
                    style: const TextStyle(
                      color: AppTheme.primaryColor,
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                CopyButtonWidget(textToCopy: url.shortUrl),
                IconButton(
                  icon: const Icon(LucideIcons.qrCode, color: AppTheme.textGrey, size: 20),
                  onPressed: () {
                    showModalBottomSheet(
                      context: context,
                      isScrollControlled: true,
                      backgroundColor: Colors.transparent,
                      builder: (context) => QrCodeModal(shortUrl: url.shortUrl),
                    );
                  },
                  tooltip: 'Show QR Code',
                ),
                IconButton(
                  icon: const Icon(LucideIcons.externalLink, color: AppTheme.textGrey, size: 20),
                  onPressed: _openUrl,
                  tooltip: 'Open in browser',
                ),
                if (showDelete)
                  IconButton(
                    icon: const Icon(LucideIcons.trash2, color: Colors.redAccent, size: 20),
                    onPressed: onDelete,
                    tooltip: 'Delete',
                  ),
              ],
            ),
            const Gap(8),
            Text(
              url.longUrl,
              style: const TextStyle(
                color: AppTheme.textGrey,
                fontSize: 14,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const Gap(12),
            Row(
              children: [
                const Icon(LucideIcons.barChart2, color: AppTheme.textGrey, size: 16),
                const Gap(4),
                Text(
                  '${url.clickCount} clicks',
                  style: const TextStyle(color: AppTheme.textGrey, fontSize: 12),
                ),
                const Spacer(),
                if (url.createdAt != null) ...[
                  const Icon(LucideIcons.calendar, color: AppTheme.textGrey, size: 16),
                  const Gap(4),
                  Text(
                    '${url.createdAt!.day}/${url.createdAt!.month}/${url.createdAt!.year}',
                    style: const TextStyle(color: AppTheme.textGrey, fontSize: 12),
                  ),
                ]
              ],
            )
          ],
        ),
      ),
    );
  }
}
