import 'package:translator_keyboard/features/translation/domain/entities/translation_result.dart';

class TranslationModel extends TranslationResult {
  const TranslationModel({
    required super.translatedText,
    required super.sourceLangCode,
    required super.targetLangCode,
  });

  /// Parses the MyMemory API response format:
  /// {
  ///   "responseData": { "translatedText": "..." },
  ///   "responseStatus": 200
  /// }
  factory TranslationModel.fromMyMemoryJson(
    Map<String, dynamic> json, {
    required String sourceLangCode,
    required String targetLangCode,
  }) {
    final responseData = json['responseData'] as Map<String, dynamic>;
    return TranslationModel(
      translatedText: responseData['translatedText'] as String? ?? '',
      sourceLangCode: sourceLangCode,
      targetLangCode: targetLangCode,
    );
  }
}
