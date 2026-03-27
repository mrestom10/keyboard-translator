import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:translator_keyboard/core/di/injection.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/translate_text.dart';

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

  const channel = MethodChannel('translator_keyboard/translation');

  channel.setMethodCallHandler((call) async {
    if (call.method == 'translate') {
      try {
        final args = Map<String, dynamic>.from(call.arguments as Map);
        final text = args['text'] as String;
        final targetLang = args['targetLang'] as String;

        // Use 'auto' as source — MyMemory supports 'autodetect',
        // Google supports 'auto'. The data source handles both.
        const sourceLang = 'auto';

        // Translate with timeout
        final result = await translateText(TranslateParams(
          text: text,
          sourceLangCode: sourceLang,
          targetLangCode: targetLang,
        )).timeout(const Duration(seconds: 10));

        return result.fold(
          (failure) => {'error': failure.message},
          (translation) => {
            'translatedText': translation.translatedText,
            'detectedLang': translation.sourceLangCode,
            'targetLang': translation.targetLangCode,
          },
        );
      } on TimeoutException {
        return {'error': 'Translation timed out'};
      } catch (e) {
        return {'error': '$e'};
      }
    }
    throw MissingPluginException('Unknown method: ${call.method}');
  });

  debugPrint('KEYBOARD: Translation service ready');
}
