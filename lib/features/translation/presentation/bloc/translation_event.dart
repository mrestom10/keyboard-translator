import 'package:equatable/equatable.dart';
import 'package:translator_keyboard/features/translation/domain/entities/language.dart';

abstract class TranslationEvent extends Equatable {
  const TranslationEvent();

  @override
  List<Object?> get props => [];
}

class TextChangedEvent extends TranslationEvent {
  final String text;

  const TextChangedEvent(this.text);

  @override
  List<Object?> get props => [text];
}

class TargetLanguageChanged extends TranslationEvent {
  final Language language;

  const TargetLanguageChanged(this.language);

  @override
  List<Object?> get props => [language];
}

class SwapLanguagesRequested extends TranslationEvent {
  const SwapLanguagesRequested();
}

class ClearTextRequested extends TranslationEvent {
  const ClearTextRequested();
}

/// Internal event triggered after debounce
class TriggerTranslation extends TranslationEvent {
  final String text;

  const TriggerTranslation(this.text);

  @override
  List<Object?> get props => [text];
}
