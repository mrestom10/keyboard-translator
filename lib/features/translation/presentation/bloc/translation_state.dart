import 'package:equatable/equatable.dart';
import 'package:translator_keyboard/features/translation/domain/entities/language.dart';
import 'package:translator_keyboard/languages/language_registry.dart';

enum TranslationStatus { idle, loading, success, failure }

class TranslationState extends Equatable {
  final String inputText;
  final String outputText;
  final Language? detectedSourceLanguage;
  final Language targetLanguage;
  final TranslationStatus status;
  final String? errorMessage;

  const TranslationState({
    required this.inputText,
    required this.outputText,
    required this.detectedSourceLanguage,
    required this.targetLanguage,
    required this.status,
    this.errorMessage,
  });

  factory TranslationState.initial() => TranslationState(
        inputText: '',
        outputText: '',
        detectedSourceLanguage: null,
        targetLanguage: LanguageRegistry.defaultTarget,
        status: TranslationStatus.idle,
      );

  bool get isInputRTL => detectedSourceLanguage?.isRTL ?? false;
  bool get isOutputRTL => targetLanguage.isRTL;

  TranslationState copyWith({
    String? inputText,
    String? outputText,
    Language? detectedSourceLanguage,
    Language? targetLanguage,
    TranslationStatus? status,
    String? errorMessage,
  }) {
    return TranslationState(
      inputText: inputText ?? this.inputText,
      outputText: outputText ?? this.outputText,
      detectedSourceLanguage:
          detectedSourceLanguage ?? this.detectedSourceLanguage,
      targetLanguage: targetLanguage ?? this.targetLanguage,
      status: status ?? this.status,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }

  @override
  List<Object?> get props => [
        inputText,
        outputText,
        detectedSourceLanguage,
        targetLanguage,
        status,
        errorMessage,
      ];
}
