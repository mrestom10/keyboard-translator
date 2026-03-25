import 'package:equatable/equatable.dart';

class TranslationResult extends Equatable {
  final String translatedText;
  final String sourceLangCode;
  final String targetLangCode;

  const TranslationResult({
    required this.translatedText,
    required this.sourceLangCode,
    required this.targetLangCode,
  });

  @override
  List<Object> get props => [translatedText, sourceLangCode, targetLangCode];
}
