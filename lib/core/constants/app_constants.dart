class AppConstants {
  AppConstants._();

  static const String appName = 'Translator Keyboard';
  static const String methodChannelName = 'translator_keyboard/actions';

  // API
  static const String myMemoryBaseUrl =
      'https://api.mymemory.translated.net/get';
  static const String libreTranslateBaseUrl = 'https://libretranslate.com';

  // Debounce
  static const int debounceDurationMs = 400;

  // UI
  static const double keyboardPanelHeight = 380;
  static const int minCharsForTranslation = 2;
}
