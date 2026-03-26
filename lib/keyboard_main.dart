import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:translator_keyboard/core/di/injection.dart';
import 'package:translator_keyboard/core/theme/app_theme.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_bloc.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/keyboard_panel.dart';

/// Named entry point for the keyboard extension.
/// Called from native Android IME and iOS Keyboard Extension.
@pragma('vm:entry-point')
void keyboardMain() {
  WidgetsFlutterBinding.ensureInitialized();
  configureDependencies();

  FlutterError.onError = (details) {
    debugPrint('Flutter error in keyboard: ${details.exception}');
  };

  runApp(const KeyboardPanelApp());
}

class KeyboardPanelApp extends StatelessWidget {
  const KeyboardPanelApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: ThemeMode.system,
      builder: (context, child) {
        // Catch widget-level errors and show fallback
        ErrorWidget.builder = (FlutterErrorDetails details) {
          debugPrint('Widget error: ${details.exception}');
          return Container(
            color: Colors.white,
            child: Center(
              child: Text(
                'Error: ${details.exception}',
                style: const TextStyle(color: Colors.red, fontSize: 12),
              ),
            ),
          );
        };
        return child ?? const SizedBox.shrink();
      },
      home: BlocProvider(
        create: (_) => getIt<TranslationBloc>(),
        child: const KeyboardPanel(),
      ),
    );
  }
}
