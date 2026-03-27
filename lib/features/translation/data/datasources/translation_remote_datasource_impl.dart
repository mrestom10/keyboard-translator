import 'package:dio/dio.dart';
import 'package:flutter/widgets.dart';
import 'package:translator_keyboard/core/constants/app_constants.dart';
import 'package:translator_keyboard/core/errors/exceptions.dart';
import 'package:translator_keyboard/features/translation/data/datasources/translation_remote_datasource.dart';
import 'package:translator_keyboard/features/translation/data/models/translation_model.dart';

class TranslationRemoteDataSourceImpl implements TranslationRemoteDataSource {
  final Dio _dio;

  TranslationRemoteDataSourceImpl(this._dio);

  @override
  Future<TranslationModel> translateText({
    required String text,
    required String sourceLangCode,
    required String targetLangCode,
  }) async {
    // Try MyMemory first, then Google Translate as fallback
    try {
      return await _translateWithMyMemory(text, sourceLangCode, targetLangCode);
    } catch (e) {
      debugPrint('MyMemory failed: $e — trying Google fallback');
    }

    try {
      return await _translateWithGoogle(text, sourceLangCode, targetLangCode);
    } catch (e) {
      debugPrint('Google fallback also failed: $e');
      throw ServerException(message: 'All translation services failed');
    }
  }

  /// MyMemory API — supports 'autodetect' as source language
  Future<TranslationModel> _translateWithMyMemory(
    String text,
    String sourceLangCode,
    String targetLangCode,
  ) async {
    try {
      // MyMemory supports 'autodetect' as source language for auto-detection
      final langPair = sourceLangCode == 'auto'
          ? 'autodetect|$targetLangCode'
          : '$sourceLangCode|$targetLangCode';

      final response = await _dio.get(
        AppConstants.myMemoryBaseUrl,
        queryParameters: {
          'q': text,
          'langpair': langPair,
          'de': 'translator-keyboard@app.com', // 50K chars/day with email
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data as Map<String, dynamic>;
        final responseStatus = data['responseStatus'];

        if (responseStatus == 200) {
          return TranslationModel.fromMyMemoryJson(
            data,
            sourceLangCode: sourceLangCode,
            targetLangCode: targetLangCode,
          );
        }
      }

      throw const ServerException(message: 'MyMemory returned bad response');
    } on DioException catch (e) {
      throw ServerException(
        message: e.message ?? 'MyMemory network error',
        statusCode: e.response?.statusCode,
      );
    }
  }

  /// Google Translate unofficial endpoint — reliable fallback
  Future<TranslationModel> _translateWithGoogle(
    String text,
    String sourceLangCode,
    String targetLangCode,
  ) async {
    try {
      final response = await _dio.get(
        'https://translate.googleapis.com/translate_a/single',
        queryParameters: {
          'client': 'gtx',
          'sl': sourceLangCode == 'auto' ? 'auto' : sourceLangCode,
          'tl': targetLangCode,
          'dt': 't',
          'q': text,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        // Response is nested array: [[["translated","original",...],...],null,"detected_lang"]
        final data = response.data;
        if (data is List && data.isNotEmpty && data[0] is List) {
          final translations = data[0] as List;
          final buffer = StringBuffer();
          for (final segment in translations) {
            if (segment is List && segment.isNotEmpty) {
              buffer.write(segment[0]?.toString() ?? '');
            }
          }
          final translated = buffer.toString();
          if (translated.isNotEmpty) {
            final detectedLang = (data.length > 2 && data[2] is String)
                ? data[2] as String
                : sourceLangCode;
            return TranslationModel(
              translatedText: translated,
              sourceLangCode: detectedLang,
              targetLangCode: targetLangCode,
            );
          }
        }
      }

      throw const ServerException(message: 'Google Translate returned bad response');
    } on DioException catch (e) {
      throw ServerException(
        message: e.message ?? 'Google Translate network error',
        statusCode: e.response?.statusCode,
      );
    }
  }

  @override
  Future<String> detectLanguage(String text) async {
    // Use Google Translate for detection — it's built into the translate call
    // We do a translate to 'en' with source='auto' and read the detected language
    try {
      final response = await _dio.get(
        'https://translate.googleapis.com/translate_a/single',
        queryParameters: {
          'client': 'gtx',
          'sl': 'auto',
          'tl': 'en',
          'dt': 't',
          'q': text,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data;
        if (data is List && data.length > 2 && data[2] is String) {
          return data[2] as String;
        }
      }
    } catch (e) {
      debugPrint('Language detection failed: $e');
    }

    // Fallback: return 'en'
    return 'en';
  }
}
