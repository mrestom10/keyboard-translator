import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:translator_keyboard/core/di/injection.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/detect_language.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/translate_text.dart';
import 'package:translator_keyboard/languages/language_registry.dart';

/// Headless entry point for the keyboard extension.
/// No UI rendering — only provides translation services via MethodChannel.
/// The keyboard UI is rendered natively by Android for reliability.
@pragma('vm:entry-point')
void keyboardMain() {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    configureDependencies();
  } catch (e) {
    debugPrint('KEYBOARD: DI init failed: $e');
    return;
  }

  final translateText = getIt<TranslateText>();
  final detectLanguage = getIt<DetectLanguage>();

  const channel = MethodChannel('translator_keyboard/translation');

  channel.setMethodCallHandler((call) async {
    if (call.method == 'translate') {
      try {
        final args = Map<String, dynamic>.from(call.arguments as Map);
        final text = args['text'] as String;
        final targetLang = args['targetLang'] as String;

        // 1. Try to detect source language (with short timeout — LibreTranslate
        //    is often unreachable so don't block on it)
        String sourceLang = 'en'; // safe default
        try {
          final detected = await detectLanguage(text)
              .timeout(const Duration(seconds: 3));
          sourceLang = detected.fold((_) => 'en', (code) => code);
        } catch (_) {
          // Detection failed/timed out — use 'en' as default source
          sourceLang = 'en';
        }

        // 2. If source == target, pick a different source
        if (sourceLang == targetLang) {
          final alt = LanguageRegistry.supported.firstWhere(
            (l) => l.code != targetLang,
            orElse: () => LanguageRegistry.defaultSource,
          );
          sourceLang = alt.code;
        }

        // 3. Translate (with timeout)
        final result = await translateText(TranslateParams(
          text: text,
          sourceLangCode: sourceLang,
          targetLangCode: targetLang,
        )).timeout(const Duration(seconds: 8));

        return result.fold(
          (failure) => {'error': failure.message},
          (translation) => {
            'translatedText': translation.translatedText,
            'detectedLang': sourceLang,
            'targetLang': targetLang,
          },
        );
      } on TimeoutException {
        return {'error': 'Translation timed out'};
      } catch (e) {
        return {'error': 'Translation failed: $e'};
      }
    }
    throw MissingPluginException('Unknown method: ${call.method}');
  });

  debugPrint('KEYBOARD: Translation service ready');
}
