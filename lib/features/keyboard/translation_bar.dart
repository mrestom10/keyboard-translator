import 'package:flutter/material.dart';

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
  final VoidCallback onCycleTargetLanguage;

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
    required this.onCycleTargetLanguage,
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
                // Source language chip
                _LangChip(
                  flag: sourceLangFlag,
                  name: sourceLangName,
                  color: colorScheme.surfaceContainerHighest,
                  textColor: colorScheme.onSurfaceVariant,
                ),
                // Swap button
                GestureDetector(
                  onTap: onSwapLanguages,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 6),
                    child: Icon(Icons.swap_horiz,
                        size: 18, color: colorScheme.primary),
                  ),
                ),
                // Target language chip (tap to cycle)
                GestureDetector(
                  onTap: onCycleTargetLanguage,
                  child: _LangChip(
                    flag: targetLangFlag,
                    name: targetLangName,
                    color: colorScheme.primaryContainer,
                    textColor: colorScheme.onPrimaryContainer,
                    showArrow: true,
                  ),
                ),
                const Spacer(),
                // Clear button
                if (inputText.isNotEmpty)
                  GestureDetector(
                    onTap: onClear,
                    child:
                        Icon(Icons.clear, size: 16, color: colorScheme.error),
                  ),
              ],
            ),
          ),

          // Input text preview
          if (inputText.isNotEmpty)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 3),
              constraints: const BoxConstraints(maxHeight: 36),
              child: Text(
                inputText,
                style: TextStyle(fontSize: 13, color: colorScheme.onSurface),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),

          // Translation output
          if (inputText.isNotEmpty) ...[
            Divider(height: 1, color: colorScheme.outlineVariant),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 3),
              constraints: const BoxConstraints(maxHeight: 40),
              child: _buildOutput(context),
            ),
          ],

          // Send buttons row
          if (outputText.isNotEmpty && !isLoading)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
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
      return Row(
        children: [
          SizedBox(
            width: 14,
            height: 14,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: colorScheme.primary,
            ),
          ),
          const SizedBox(width: 8),
          Text(
            'Translating...',
            style: TextStyle(
              fontSize: 12,
              color: colorScheme.onSurfaceVariant,
            ),
          ),
        ],
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
        'Translation appears here',
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

class _LangChip extends StatelessWidget {
  final String flag;
  final String name;
  final Color color;
  final Color textColor;
  final bool showArrow;

  const _LangChip({
    required this.flag,
    required this.name,
    required this.color,
    required this.textColor,
    this.showArrow = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(flag, style: const TextStyle(fontSize: 14)),
          const SizedBox(width: 4),
          Text(
            name,
            style: TextStyle(fontSize: 11, color: textColor),
          ),
          if (showArrow) ...[
            const SizedBox(width: 2),
            Icon(Icons.unfold_more, size: 12, color: textColor),
          ],
        ],
      ),
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
              style: TextStyle(
                  fontSize: 11, color: color, fontWeight: FontWeight.w500),
            ),
          ],
        ),
      ),
    );
  }
}
