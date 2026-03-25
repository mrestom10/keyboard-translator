import 'dart:io';

import 'package:flutter/material.dart';
import 'package:gap/gap.dart';

class SetupGuidePage extends StatelessWidget {
  const SetupGuidePage({super.key});

  @override
  Widget build(BuildContext context) {
    final isAndroid = Platform.isAndroid;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Setup Guide'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Icon(
            isAndroid ? Icons.android : Icons.apple,
            size: 48,
            color: Theme.of(context).colorScheme.primary,
          ),
          const Gap(16),
          Text(
            isAndroid ? 'Android Setup' : 'iOS Setup',
            style: Theme.of(context).textTheme.headlineSmall,
            textAlign: TextAlign.center,
          ),
          const Gap(24),
          if (isAndroid) ..._androidSteps(context) else ..._iosSteps(context),
        ],
      ),
    );
  }

  List<Widget> _androidSteps(BuildContext context) {
    return [
      _StepCard(
        step: 1,
        title: 'Open Keyboard Settings',
        description:
            'Go to Settings \u2192 General Management \u2192 Keyboard \u2192 On-screen keyboard',
        icon: Icons.settings,
      ),
      const Gap(12),
      _StepCard(
        step: 2,
        title: 'Enable Translator Keyboard',
        description: 'Toggle on Translator Keyboard in the list',
        icon: Icons.toggle_on,
      ),
      const Gap(12),
      _StepCard(
        step: 3,
        title: 'Grant Permissions',
        description: 'Grant all requested permissions when prompted',
        icon: Icons.security,
      ),
      const Gap(12),
      _StepCard(
        step: 4,
        title: 'Switch to Translator Keyboard',
        description:
            'Open any app and long-press the keyboard icon to switch to Translator Keyboard',
        icon: Icons.swap_horiz,
      ),
    ];
  }

  List<Widget> _iosSteps(BuildContext context) {
    return [
      _StepCard(
        step: 1,
        title: 'Add Keyboard',
        description:
            'Go to Settings \u2192 General \u2192 Keyboard \u2192 Keyboards \u2192 Add New Keyboard',
        icon: Icons.add,
      ),
      const Gap(12),
      _StepCard(
        step: 2,
        title: 'Select Translator Keyboard',
        description: 'Find and select Translator Keyboard from the list',
        icon: Icons.check_circle,
      ),
      const Gap(12),
      _StepCard(
        step: 3,
        title: 'Allow Full Access',
        description:
            'Tap Translator Keyboard and enable "Allow Full Access" (required for translations)',
        icon: Icons.lock_open,
      ),
      const Gap(12),
      _StepCard(
        step: 4,
        title: 'Switch Keyboards',
        description:
            'Tap the globe icon while typing in any app to switch to Translator Keyboard',
        icon: Icons.language,
      ),
    ];
  }
}

class _StepCard extends StatelessWidget {
  final int step;
  final String title;
  final String description;
  final IconData icon;

  const _StepCard({
    required this.step,
    required this.title,
    required this.description,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: colorScheme.primaryContainer,
              foregroundColor: colorScheme.onPrimaryContainer,
              child: Text('$step'),
            ),
            const Gap(16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(icon, size: 18, color: colorScheme.primary),
                      const Gap(8),
                      Text(
                        title,
                        style:
                            Theme.of(context).textTheme.titleSmall?.copyWith(
                                  fontWeight: FontWeight.w600,
                                ),
                      ),
                    ],
                  ),
                  const Gap(4),
                  Text(
                    description,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
