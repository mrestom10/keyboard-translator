import 'package:flutter/material.dart';
import 'package:gap/gap.dart';
import 'package:translator_keyboard/features/translation/domain/entities/language.dart';
import 'package:translator_keyboard/languages/language_registry.dart';

class LanguageSelectorRow extends StatelessWidget {
  final Language? sourceLanguage;
  final Language targetLanguage;
  final ValueChanged<Language> onTargetChanged;
  final VoidCallback onSwap;

  const LanguageSelectorRow({
    super.key,
    required this.sourceLanguage,
    required this.targetLanguage,
    required this.onTargetChanged,
    required this.onSwap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Row(
        children: [
          // Source language (auto-detected, read-only)
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    sourceLanguage?.flagEmoji ?? '',
                    style: const TextStyle(fontSize: 16),
                  ),
                  const Gap(4),
                  Flexible(
                    child: Text(
                      sourceLanguage?.name ?? 'Auto-detect',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Swap button
          IconButton(
            onPressed: onSwap,
            icon: Icon(
              Icons.swap_horiz,
              color: colorScheme.primary,
              size: 20,
            ),
            constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
            padding: EdgeInsets.zero,
          ),

          // Target language (dropdown)
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              decoration: BoxDecoration(
                color: colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(8),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<Language>(
                  value: targetLanguage,
                  isExpanded: true,
                  isDense: true,
                  icon: Icon(
                    Icons.arrow_drop_down,
                    color: colorScheme.onPrimaryContainer,
                    size: 18,
                  ),
                  items: LanguageRegistry.supported.map((lang) {
                    return DropdownMenuItem<Language>(
                      value: lang,
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(lang.flagEmoji,
                              style: const TextStyle(fontSize: 16)),
                          const Gap(4),
                          Flexible(
                            child: Text(
                              lang.name,
                              style: Theme.of(context)
                                  .textTheme
                                  .bodySmall
                                  ?.copyWith(
                                    color: colorScheme.onPrimaryContainer,
                                  ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                  onChanged: (lang) {
                    if (lang != null) onTargetChanged(lang);
                  },
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
