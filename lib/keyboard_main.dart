import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:dio/dio.dart';

/// Headless entry point for the keyboard extension.
/// Makes translation API calls directly — no DI, no repository layers,
/// no connectivity_plus (hangs in headless engine).
@pragma('vm:entry-point')
void keyboardMain() {
  WidgetsFlutterBinding.ensureInitialized();

  // Minimal runApp to keep the Dart event loop pumping platform messages.
  // Without this, MethodChannel calls from native may never be delivered.
  runApp(const SizedBox.shrink());

  final dio = Dio(BaseOptions(
    connectTimeout: const Duration(seconds: 8),
    receiveTimeout: const Duration(seconds: 8),
    sendTimeout: const Duration(seconds: 8),
  ));

  const channel = MethodChannel('translator_keyboard/translation');

  channel.setMethodCallHandler((call) async {
    if (call.method == 'translate') {
      try {
        final args = Map<String, dynamic>.from(call.arguments as Map);
        final text = args['text'] as String;
        final targetLang = args['targetLang'] as String;

        debugPrint('KEYBOARD: Translating "$text" -> $targetLang');

        // Try MyMemory first
        try {
          final result = await _translateMyMemory(dio, text, targetLang);
          if (result != null) return result;
        } catch (e) {
          debugPrint('KEYBOARD: MyMemory failed: $e');
        }

        // Fallback to Google Translate
        try {
          final result = await _translateGoogle(dio, text, targetLang);
          if (result != null) return result;
        } catch (e) {
          debugPrint('KEYBOARD: Google fallback failed: $e');
        }

        return {'error': 'Translation services unavailable'};
      } catch (e) {
        debugPrint('KEYBOARD: Translate error: $e');
        return {'error': '$e'};
      }
    }
    return {'error': 'Unknown method: ${call.method}'};
  });

  debugPrint('KEYBOARD: Translation service ready');
}

/// MyMemory API — free, supports autodetect
Future<Map<String, String>?> _translateMyMemory(
    Dio dio, String text, String targetLang) async {
  final response = await dio.get(
    'https://api.mymemory.translated.net/get',
    queryParameters: {
      'q': text,
      'langpair': 'autodetect|$targetLang',
      'de': 'translator-keyboard@app.com',
    },
  );

  if (response.statusCode == 200 && response.data != null) {
    final data = response.data;
    final Map<String, dynamic> json = data is String
        ? jsonDecode(data) as Map<String, dynamic>
        : data as Map<String, dynamic>;

    final status = json['responseStatus'];
    if (status == 200 || status == '200') {
      final responseData = json['responseData'] as Map<String, dynamic>;
      final translated = responseData['translatedText'] as String? ?? '';
      if (translated.isNotEmpty && translated != text) {
        return {
          'translatedText': translated,
          'detectedLang': 'auto',
          'targetLang': targetLang,
        };
      }
    }
  }
  return null;
}

/// Google Translate unofficial — reliable fallback
Future<Map<String, String>?> _translateGoogle(
    Dio dio, String text, String targetLang) async {
  final response = await dio.get(
    'https://translate.googleapis.com/translate_a/single',
    queryParameters: {
      'client': 'gtx',
      'sl': 'auto',
      'tl': targetLang,
      'dt': 't',
      'q': text,
    },
  );

  if (response.statusCode == 200 && response.data != null) {
    final data = response.data;
    if (data is List && data.isNotEmpty && data[0] is List) {
      final segments = data[0] as List;
      final buffer = StringBuffer();
      for (final seg in segments) {
        if (seg is List && seg.isNotEmpty) {
          buffer.write(seg[0]?.toString() ?? '');
        }
      }
      final translated = buffer.toString();
      if (translated.isNotEmpty) {
        final detected =
            (data.length > 2 && data[2] is String) ? data[2] as String : 'auto';
        return {
          'translatedText': translated,
          'detectedLang': detected,
          'targetLang': targetLang,
        };
      }
    }
  }
  return null;
}
