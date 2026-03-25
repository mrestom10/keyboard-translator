import 'package:dartz/dartz.dart';
import 'package:translator_keyboard/core/errors/failures.dart';
import 'package:translator_keyboard/features/translation/domain/entities/translation_result.dart';

abstract class TranslationRepository {
  Future<Either<Failure, TranslationResult>> translateText({
    required String text,
    required String sourceLangCode,
    required String targetLangCode,
  });

  Future<Either<Failure, String>> detectLanguage(String text);
}
