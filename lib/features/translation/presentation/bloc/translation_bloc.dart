import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:translator_keyboard/core/constants/app_constants.dart';
import 'package:translator_keyboard/core/utils/debouncer.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/detect_language.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/translate_text.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_event.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_state.dart';
import 'package:translator_keyboard/languages/language_registry.dart';

class TranslationBloc extends Bloc<TranslationEvent, TranslationState> {
  final TranslateText _translateText;
  final DetectLanguage _detectLanguage;
  final Debouncer _debouncer;

  TranslationBloc({
    required TranslateText translateText,
    required DetectLanguage detectLanguage,
    required Debouncer debouncer,
  })  : _translateText = translateText,
        _detectLanguage = detectLanguage,
        _debouncer = debouncer,
        super(TranslationState.initial()) {
    on<TextChangedEvent>(_onTextChanged);
    on<TriggerTranslation>(_onTriggerTranslation);
    on<TargetLanguageChanged>(_onTargetLanguageChanged);
    on<SwapLanguagesRequested>(_onSwapLanguages);
    on<ClearTextRequested>(_onClearText);
  }

  void _onTextChanged(
    TextChangedEvent event,
    Emitter<TranslationState> emit,
  ) {
    emit(state.copyWith(inputText: event.text));

    if (event.text.trim().length < AppConstants.minCharsForTranslation) {
      return;
    }

    _debouncer.run(() {
      add(TriggerTranslation(event.text));
    });
  }

  Future<void> _onTriggerTranslation(
    TriggerTranslation event,
    Emitter<TranslationState> emit,
  ) async {
    emit(state.copyWith(status: TranslationStatus.loading));

    // 1. Auto-detect source language
    final detected = await _detectLanguage(event.text);
    final sourceLang = detected.fold(
      (_) => null,
      (code) => LanguageRegistry.findByCode(code),
    );

    // 2. Guard: if source == target, auto-pick next language
    final target = sourceLang?.code == state.targetLanguage.code
        ? LanguageRegistry.supported
            .firstWhere((l) => l.code != sourceLang!.code)
        : state.targetLanguage;

    // 3. Translate
    final result = await _translateText(TranslateParams(
      text: event.text,
      sourceLangCode: sourceLang?.code ?? 'auto',
      targetLangCode: target.code,
    ));

    result.fold(
      (failure) => emit(state.copyWith(
        status: TranslationStatus.failure,
        errorMessage: failure.message,
      )),
      (translation) => emit(state.copyWith(
        status: TranslationStatus.success,
        outputText: translation.translatedText,
        detectedSourceLanguage: sourceLang,
        targetLanguage: target,
      )),
    );
  }

  void _onTargetLanguageChanged(
    TargetLanguageChanged event,
    Emitter<TranslationState> emit,
  ) {
    emit(state.copyWith(targetLanguage: event.language));

    if (state.inputText.trim().length >= AppConstants.minCharsForTranslation) {
      add(TriggerTranslation(state.inputText));
    }
  }

  void _onSwapLanguages(
    SwapLanguagesRequested event,
    Emitter<TranslationState> emit,
  ) {
    if (state.outputText.isEmpty || state.detectedSourceLanguage == null) {
      return;
    }

    final newInput = state.outputText;
    emit(state.copyWith(
      inputText: newInput,
      outputText: state.inputText,
      targetLanguage: state.detectedSourceLanguage!,
      detectedSourceLanguage: state.targetLanguage,
    ));

    add(TriggerTranslation(newInput));
  }

  void _onClearText(
    ClearTextRequested event,
    Emitter<TranslationState> emit,
  ) {
    emit(TranslationState.initial());
  }

  @override
  Future<void> close() {
    _debouncer.dispose();
    return super.close();
  }
}
