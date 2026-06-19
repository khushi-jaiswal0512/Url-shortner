import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../theme/app_theme.dart';

class CopyButtonWidget extends StatefulWidget {
  final String textToCopy;

  const CopyButtonWidget({Key? key, required this.textToCopy}) : super(key: key);

  @override
  State<CopyButtonWidget> createState() => _CopyButtonWidgetState();
}

class _CopyButtonWidgetState extends State<CopyButtonWidget> {
  bool _isCopied = false;

  void _copy() async {
    await Clipboard.setData(ClipboardData(text: widget.textToCopy));
    setState(() {
      _isCopied = true;
    });

    Fluttertoast.showToast(
      msg: "Copied to clipboard!",
      toastLength: Toast.LENGTH_SHORT,
      gravity: ToastGravity.BOTTOM,
      backgroundColor: AppTheme.primaryColor,
      textColor: Colors.white,
      fontSize: 14.0,
    );

    Future.delayed(const Duration(seconds: 2), () {
      if (mounted) {
        setState(() {
          _isCopied = false;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: _isCopied
          ? const Icon(LucideIcons.check, color: Colors.green)
              .animate()
              .scale(duration: 200.ms, curve: Curves.easeOutBack)
              .fade()
          : const Icon(LucideIcons.copy, color: AppTheme.textGrey)
              .animate()
              .fade(duration: 200.ms),
      onPressed: _copy,
      tooltip: 'Copy URL',
    );
  }
}
