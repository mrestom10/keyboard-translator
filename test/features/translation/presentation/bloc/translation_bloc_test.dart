import 'package:bloc_test/bloc_test.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:translator_keyboard/core/errors/failures.dart';
import 'package:translator_keyboard/core/utils/debouncer.dart';
import 'package:translator_keyboard/features/translation/domain/entities/translation_result.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/detect_language.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/translate_text.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_bloc.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_event.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_state.dart';

class MockTranslateText extends Mock implements TranslateText {}

class MockDetectLanguage extends Mock implements DetectLanguage {}

class FakeTranslateParams extends Fake implements TranslateParams {}

void main() {
  late TranslationBloc bloc;
  late MockTranslateText mockTranslateText;
  late MockDetectLanguage mockDetectLanguage;

  setUpAll(() {
    registerFallbackValue(FakeTranslateParams());
  });

  setUp(() {
    mockTranslateText = MockTranslateText();
    mockDetectLanguage = MockDetectLanguage();
    bloc = TranslationBloc(
      translateText: mockTranslateText,
      detectLanguage: mockDetectLanguage,
      debouncer: Debouncer(milliseconds: 0), // instant for tests
    );
  });

  tearDown(() {
    bloc.close();
  });

  test('initial state is correct', () {
    expect(bloc.state, TranslationState.initial());
  });

  blocTest<TranslationBloc, TranslationState>(
    'emits updated inputText on TextChangedEvent',
    build: () => bloc,
    act: (bloc) => bloc.add(const TextChangedEvent('h')),
    expect: () => [
      TranslationState.initial().copyWith(inputText: 'h'),
    ],
  );

  blocTest<TranslationBloc, TranslationState>(
    'emits initial state on ClearTextRequested',
    build: () => bloc,
    seed: () => TranslationState.initial().copyWith(inputText: 'hello'),
    act: (bloc) => bloc.add(const ClearTextRequested()),
    expect: () => [TranslationState.initial()],
  );

  blocTest<TranslationBloc, TranslationState>(
    'emits loading then success on TriggerTranslation',
    build: () {
      when(() => mockDetectLanguage(any()))
          .thenAnswer((_) async => const Right('en'));
      when(() => mockTranslateText(any())).thenAnswer(
        (_) async => const Right(TranslationResult(
          translatedText: 'مرحبا',
          sourceLangCode: 'en',
          targetLangCode: 'ar',
        )),
      );
      return bloc;
    },
    act: (bloc) => bloc.add(const TriggerTranslation('hello')),
    expect: () => [
      isA<TranslationState>()
          .having((s) => s.status, 'status', TranslationStatus.loading),
      isA<TranslationState>()
          .having((s) => s.status, 'status', TranslationStatus.success)
          .having((s) => s.outputText, 'outputText', 'مرحبا'),
    ],
  );

  blocTest<TranslationBloc, TranslationState>(
    'emits failure when translation fails',
    build: () {
      when(() => mockDetectLanguage(any()))
          .thenAnswer((_) async => const Right('en'));
      when(() => mockTranslateText(any()))
          .thenAnswer((_) async => const Left(ServerFailure('API error')));
      return bloc;
    },
    act: (bloc) => bloc.add(const TriggerTranslation('hello')),
    expect: () => [
      isA<TranslationState>()
          .having((s) => s.status, 'status', TranslationStatus.loading),
      isA<TranslationState>()
          .having((s) => s.status, 'status', TranslationStatus.failure)
          .having((s) => s.errorMessage, 'errorMessage', 'API error'),
    ],
  );
}
