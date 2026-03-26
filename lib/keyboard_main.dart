import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:translator_keyboard/core/di/injection.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_bloc.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/keyboard_panel.dart';

/// Named entry point for the keyboard extension.
/// Called from native Android IME and iOS Keyboard Extension.
@pragma('vm:entry-point')
void keyboardMain() {
  WidgetsFlutterBinding.ensureInitialized();

  FlutterError.onError = (details) {
    debugPrint('KEYBOARD_ERROR: ${details.exception}');
    debugPrint('KEYBOARD_STACK: ${details.stack}');
  };

  // Try to init DI — if it fails, still show the keyboard
  try {
    configureDependencies();
  } catch (e) {
    debugPrint('KEYBOARD_DI_ERROR: $e');
  }

  runApp(const KeyboardPanelApp());
}

class KeyboardPanelApp extends StatelessWidget {
  const KeyboardPanelApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      // Inline theme — zero external dependencies, cannot fail
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        colorSchemeSeed: const Color(0xFF4A90D9),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorSchemeSeed: const Color(0xFF4A90D9),
      ),
      themeMode: ThemeMode.system,
      builder: (context, child) {
        ErrorWidget.builder = (FlutterErrorDetails details) {
          debugPrint('KEYBOARD_WIDGET_ERROR: ${details.exception}');
          return ColoredBox(
            color: const Color(0xFFFF6B6B),
            child: Center(
              child: Text(
                '${details.exception}',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 11,
                  decoration: TextDecoration.none,
                ),
              ),
            ),
          );
        };
        return child ?? const SizedBox.shrink();
      },
      home: _SafeKeyboardHome(),
    );
  }
}

/// Wraps the keyboard in error-safe initialization.
/// If BLoC fails, shows a diagnostic message instead of crashing.
class _SafeKeyboardHome extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    try {
      return BlocProvider(
        create: (_) => getIt<TranslationBloc>(),
        child: const KeyboardPanel(),
      );
    } catch (e) {
      debugPrint('KEYBOARD_BLOC_ERROR: $e');
      return ColoredBox(
        color: const Color(0xFFFF6B6B),
        child: Center(
          child: Text(
            'BLoC init failed: $e',
            style: const TextStyle(
              color: Colors.white,
              fontSize: 12,
              decoration: TextDecoration.none,
            ),
          ),
        ),
      );
    }
  }
}
