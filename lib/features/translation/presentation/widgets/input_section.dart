import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class InputSection extends StatelessWidget {
  final String text;
  final bool isRTL;
  final String? fontFamily;
  final ValueChanged<String> onChanged;

  const InputSection({
    super.key,
    required this.text,
    required this.isRTL,
    required this.fontFamily,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final textStyle = _getTextStyle(context);

    return Directionality(
      textDirection: isRTL ? TextDirection.rtl : TextDirection.ltr,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12),
        child: TextField(
          onChanged: onChanged,
          style: textStyle,
          textDirection: isRTL ? TextDirection.rtl : TextDirection.ltr,
          maxLines: null,
          expands: true,
          textAlignVertical: TextAlignVertical.top,
          decoration: InputDecoration(
            hintText: 'Type to translate...',
            hintStyle: textStyle?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
            ),
            border: InputBorder.none,
            contentPadding: const EdgeInsets.symmetric(vertical: 4),
          ),
        ),
      ),
    );
  }

  TextStyle? _getTextStyle(BuildContext context) {
    final baseStyle = Theme.of(context).textTheme.bodyMedium;
    if (fontFamily == null) return baseStyle;

    try {
      return GoogleFonts.getFont(fontFamily!, textStyle: baseStyle);
    } catch (_) {
      return baseStyle;
    }
  }
}
