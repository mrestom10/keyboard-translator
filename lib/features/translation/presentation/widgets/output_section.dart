import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class OutputSection extends StatelessWidget {
  final String text;
  final bool isRTL;
  final String fontFamily;
  final String? errorMessage;

  const OutputSection({
    super.key,
    required this.text,
    required this.isRTL,
    required this.fontFamily,
    this.errorMessage,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    if (errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            errorMessage!,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: colorScheme.error,
                ),
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    if (text.isEmpty) {
      return Center(
        child: Text(
          'Translation will appear here',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
              ),
        ),
      );
    }

    final textStyle = _getTextStyle(context);

    return Directionality(
      textDirection: isRTL ? TextDirection.rtl : TextDirection.ltr,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        child: SelectableText(
          text,
          style: textStyle?.copyWith(
            color: colorScheme.primary,
            fontWeight: FontWeight.w500,
          ),
          textDirection: isRTL ? TextDirection.rtl : TextDirection.ltr,
        ),
      ),
    );
  }

  TextStyle? _getTextStyle(BuildContext context) {
    final baseStyle = Theme.of(context).textTheme.bodyMedium;
    try {
      return GoogleFonts.getFont(fontFamily, textStyle: baseStyle);
    } catch (_) {
      return baseStyle;
    }
  }
}
