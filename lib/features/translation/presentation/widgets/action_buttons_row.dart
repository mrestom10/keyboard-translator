import 'package:flutter/material.dart';
import 'package:translator_keyboard/core/platform/keyboard_channel.dart';

class ActionButtonsRow extends StatelessWidget {
  final VoidCallback onSendTranslated;
  final VoidCallback onSendOriginal;
  final VoidCallback onClear;
  final bool canSend;

  const ActionButtonsRow({
    super.key,
    required this.onSendTranslated,
    required this.onSendOriginal,
    required this.onClear,
    required this.canSend,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      height: 44,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          // Send Translated
          Expanded(
            flex: 2,
            child: FilledButton.icon(
              onPressed: canSend ? onSendTranslated : null,
              icon: const Icon(Icons.send, size: 16),
              label: const Text('Send', style: TextStyle(fontSize: 12)),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
          ),

          const SizedBox(width: 4),

          // Send Original
          Expanded(
            flex: 2,
            child: OutlinedButton.icon(
              onPressed: canSend ? onSendOriginal : null,
              icon: const Icon(Icons.text_fields, size: 16),
              label:
                  const Text('Original', style: TextStyle(fontSize: 12)),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
          ),

          const SizedBox(width: 4),

          // Clear
          IconButton(
            onPressed: onClear,
            icon: Icon(Icons.clear, size: 18, color: colorScheme.error),
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
            padding: EdgeInsets.zero,
            tooltip: 'Clear',
          ),

          // Backspace
          IconButton(
            onPressed: () => KeyboardChannel.deleteLastChar(),
            icon: Icon(Icons.backspace_outlined,
                size: 18, color: colorScheme.onSurfaceVariant),
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
            padding: EdgeInsets.zero,
            tooltip: 'Backspace',
          ),
        ],
      ),
    );
  }
}
