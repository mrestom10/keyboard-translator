import 'package:translator_keyboard/features/translation/domain/entities/language.dart';

/// Single source of truth for all supported languages.
/// Adding a new language = ONE new entry here. Nothing else changes.
class LanguageRegistry {
  LanguageRegistry._();

  static const List<Language> supported = [
    Language(
      code: 'en',
      name: 'English',
      nativeName: 'English',
      isRTL: false,
      flagEmoji: '\u{1F1EC}\u{1F1E7}',
      fontFamily: 'Inter',
    ),
    Language(
      code: 'ar',
      name: 'Arabic',
      nativeName: '\u0627\u0644\u0639\u0631\u0628\u064A\u0629',
      isRTL: true,
      flagEmoji: '\u{1F1F8}\u{1F1E6}',
      fontFamily: 'Cairo',
    ),
    Language(
      code: 'fr',
      name: 'French',
      nativeName: 'Fran\u00E7ais',
      isRTL: false,
      flagEmoji: '\u{1F1EB}\u{1F1F7}',
      fontFamily: 'Inter',
    ),
    Language(
      code: 'de',
      name: 'German',
      nativeName: 'Deutsch',
      isRTL: false,
      flagEmoji: '\u{1F1E9}\u{1F1EA}',
      fontFamily: 'Inter',
    ),
  ];

  static Language? findByCode(String code) =>
      supported.cast<Language?>().firstWhere(
            (l) => l?.code == code,
            orElse: () => null,
          );

  static Language get defaultSource =>
      supported.firstWhere((l) => l.code == 'en');

  static Language get defaultTarget =>
      supported.firstWhere((l) => l.code == 'ar');
}
