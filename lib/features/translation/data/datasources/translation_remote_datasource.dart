import 'package:translator_keyboard/features/translation/data/models/translation_model.dart';

abstract class TranslationRemoteDataSource {
  Future<TranslationModel> translateText({
    required String text,
    required String sourceLangCode,
    required String targetLangCode,
  });

  Future<String> detectLanguage(String text);
}
