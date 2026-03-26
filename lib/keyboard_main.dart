import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:translator_keyboard/core/di/injection.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/detect_language.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/translate_text.dart';
import 'package:translator_keyboard/languages/language_registry.dart';

/// Headless entry point for the keyboard extension.
/// No UI rendering — only provides translation services via MethodChannel.
/// The keyboard UI is rendered natively by Android/iOS for reliability.
@pragma('vm:entry-point')
void keyboardMain() {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    configureDependencies();
  } catch (e) {
    debugPrint('KEYBOARD_DI_ERROR: $e');
    return;
  }

  final translateText = getIt<TranslateText>();
  final detectLanguage = getIt<DetectLanguage>();

  const channel = MethodChannel('translator_keyboard/translation');

  channel.setMethodCallHandler((call) async {
    switch (call.method) {
      case 'translate':
        final args = call.arguments as Map;
        final text = args['text'] as String;
        final targetLang = args['targetLang'] as String;

        // 1. Auto-detect source language
        final detected = await detectLanguage(text);
        var sourceLang = detected.fold((_) => 'auto', (code) => code);

        // 2. If source == target, pick a different target
        var actualTarget = targetLang;
        if (sourceLang == targetLang) {
          final alt = LanguageRegistry.supported
              .firstWhere((l) => l.code != sourceLang, orElse: () => LanguageRegistry.defaultTarget);
          actualTarget = alt.code;
        }

        // 3. Translate
        final result = await translateText(TranslateParams(
          text: text,
          sourceLangCode: sourceLang,
          targetLangCode: actualTarget,
        ));

        return result.fold(
          (failure) => {
            'error': failure.message,
          },
          (translation) => {
            'translatedText': translation.translatedText,
            'detectedLang': sourceLang,
            'targetLang': actualTarget,
          },
        );

      default:
        throw MissingPluginException('Unknown method: ${call.method}');
    }
  });

  debugPrint('KEYBOARD: Translation service ready');
}
