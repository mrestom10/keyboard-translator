import 'package:dartz/dartz.dart';
import 'package:translator_keyboard/core/errors/failures.dart';
import 'package:translator_keyboard/features/translation/domain/repositories/translation_repository.dart';

class DetectLanguage {
  final TranslationRepository repository;

  DetectLanguage(this.repository);

  Future<Either<Failure, String>> call(String text) {
    return repository.detectLanguage(text);
  }
}
