import 'dart:io';

import 'package:flutter/material.dart';
import 'package:gap/gap.dart';
import 'package:translator_keyboard/companion_app/pages/setup_guide_page.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Translator Keyboard'),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Status card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    Icon(
                      Icons.keyboard_alt_outlined,
                      size: 64,
                      color: colorScheme.primary,
                    ),
                    const Gap(16),
                    Text(
                      'Translator Keyboard',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const Gap(8),
                    Text(
                      'Type in any language, get real-time translations,\nand send them directly into any app.',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            ),

            const Gap(24),

            // Setup button
            FilledButton.icon(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const SetupGuidePage(),
                  ),
                );
              },
              icon: const Icon(Icons.settings),
              label: const Text('Setup Keyboard'),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
            ),

            const Gap(16),

            // Platform info
            Card(
              color: colorScheme.secondaryContainer,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Icon(
                      Icons.info_outline,
                      color: colorScheme.onSecondaryContainer,
                    ),
                    const Gap(12),
                    Expanded(
                      child: Text(
                        Platform.isAndroid
                            ? 'After setup, switch to Translator Keyboard by long-pressing the keyboard icon in any app.'
                            : 'After setup, tap the globe icon while typing in any app to switch keyboards.',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: colorScheme.onSecondaryContainer,
                            ),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const Spacer(),

            // Supported languages
            Text(
              'Supported Languages: English, Arabic, French, German',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
