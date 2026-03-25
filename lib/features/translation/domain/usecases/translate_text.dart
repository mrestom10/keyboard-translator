import 'package:dartz/dartz.dart';
import 'package:equatable/equatable.dart';
import 'package:translator_keyboard/core/errors/failures.dart';
import 'package:translator_keyboard/features/translation/domain/entities/translation_result.dart';
import 'package:translator_keyboard/features/translation/domain/repositories/translation_repository.dart';

class TranslateText {
  final TranslationRepository repository;

  TranslateText(this.repository);

  Future<Either<Failure, TranslationResult>> call(TranslateParams params) {
    return repository.translateText(
      text: params.text,
      sourceLangCode: params.sourceLangCode,
      targetLangCode: params.targetLangCode,
    );
  }
}

class TranslateParams extends Equatable {
  final String text;
  final String sourceLangCode;
  final String targetLangCode;

  const TranslateParams({
    required this.text,
    required this.sourceLangCode,
    required this.targetLangCode,
  });

  @override
  List<Object> get props => [text, sourceLangCode, targetLangCode];
}
