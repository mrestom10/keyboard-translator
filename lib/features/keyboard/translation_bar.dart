import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

class TranslationBar extends StatelessWidget {
  final String inputText;
  final String outputText;
  final bool isLoading;
  final String? errorMessage;
  final String sourceLangFlag;
  final String sourceLangName;
  final String targetLangFlag;
  final String targetLangName;
  final VoidCallback onSendTranslated;
  final VoidCallback onSendOriginal;
  final VoidCallback onClear;
  final VoidCallback onSwapLanguages;
  final Widget languageDropdown;

  const TranslationBar({
    super.key,
    required this.inputText,
    required this.outputText,
    required this.isLoading,
    this.errorMessage,
    required this.sourceLangFlag,
    required this.sourceLangName,
    required this.targetLangFlag,
    required this.targetLangName,
    required this.onSendTranslated,
    required this.onSendOriginal,
    required this.onClear,
    required this.onSwapLanguages,
    required this.languageDropdown,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      color: colorScheme.surface,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Language row
          Container(
            height: 36,
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: Row(
              children: [
                // Source language (auto-detected)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(sourceLangFlag, style: const TextStyle(fontSize: 14)),
                      const SizedBox(width: 4),
                      Text(
                        sourceLangName,
                        style: TextStyle(
                          fontSize: 11,
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                // Swap
                GestureDetector(
                  onTap: onSwapLanguages,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: Icon(Icons.swap_horiz,
                        size: 18, color: colorScheme.primary),
                  ),
                ),
                // Target language dropdown
                Expanded(child: languageDropdown),
                const SizedBox(width: 4),
                // Clear button
                if (inputText.isNotEmpty)
                  GestureDetector(
                    onTap: onClear,
                    child: Icon(Icons.clear, size: 16, color: colorScheme.error),
                  ),
              ],
            ),
          ),

          // Input text display
          if (inputText.isNotEmpty)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              constraints: const BoxConstraints(maxHeight: 40),
              child: Text(
                inputText,
                style: TextStyle(
                  fontSize: 13,
                  color: colorScheme.onSurface,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),

          // Translation output / loading
          if (inputText.isNotEmpty) ...[
            Divider(height: 1, color: colorScheme.outlineVariant),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              constraints: const BoxConstraints(maxHeight: 48),
              child: _buildOutput(context),
            ),
          ],

          // Send buttons
          if (outputText.isNotEmpty && !isLoading)
            Container(
              height: 32,
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: Row(
                children: [
                  Expanded(
                    child: _ActionChip(
                      icon: Icons.send,
                      label: 'Send Translation',
                      color: colorScheme.primary,
                      onTap: onSendTranslated,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: _ActionChip(
                      icon: Icons.text_fields,
                      label: 'Send Original',
                      color: colorScheme.secondary,
                      onTap: onSendOriginal,
                    ),
                  ),
                ],
              ),
            ),

          Divider(height: 1, color: colorScheme.outlineVariant),
        ],
      ),
    );
  }

  Widget _buildOutput(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    if (isLoading) {
      return Shimmer.fromColors(
        baseColor: colorScheme.surfaceContainerHighest,
        highlightColor: colorScheme.surfaceContainerLow,
        child: Container(
          height: 14,
          width: 180,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
      );
    }

    if (errorMessage != null) {
      return Text(
        errorMessage!,
        style: TextStyle(fontSize: 12, color: colorScheme.error),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      );
    }

    if (outputText.isEmpty) {
      return Text(
        'Translation will appear here',
        style: TextStyle(
          fontSize: 12,
          color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
        ),
      );
    }

    return Text(
      outputText,
      style: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w500,
        color: colorScheme.primary,
      ),
      maxLines: 2,
      overflow: TextOverflow.ellipsis,
    );
  }
}

class _ActionChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _ActionChip({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 28,
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w500),
            ),
          ],
        ),
      ),
    );
  }
}
