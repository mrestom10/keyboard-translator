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
    // Lightweight theme — no GoogleFonts (can fail in IME sandbox with no network).
    // No Scaffold — it adds unnecessary overhead and transparent-background bugs.
    return MaterialApp(
      debugShowCheckedModeBanner: false,
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
          debugPrint('Widget error: ${details.exception}');
          return Container(
            color: const Color(0xFFF0F0F3),
            child: Center(
              child: Text(
                'Keyboard error: ${details.exception}',
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
