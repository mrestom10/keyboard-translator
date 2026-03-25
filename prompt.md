# Flutter Keyboard Translation Extension

## Full AI Implementation Prompt

---

## 🎯 Project Vision

Build a **Flutter + Native hybrid keyboard translation extension** that appears as a panel **above the system keyboard** in ANY application (WhatsApp, Telegram, iMessage, etc.). The user types in their preferred language, sees a real-time translation, and taps a send/inject button to push the translated (or original) text into the active text field of any app.

This is NOT a standalone translator app. It is an **Input Method Extension** on Android and a **Custom Keyboard Extension** on iOS that hosts a Flutter UI inside a native keyboard container.

---

## 🏗️ Project Structure

```
translator_keyboard/
├── lib/                                         # Shared Flutter UI & logic
│   ├── main.dart                                # Main app entry (companion app)
│   ├── keyboard_main.dart                       # Keyboard panel entry point
│   │
│   ├── core/
│   │   ├── constants/app_constants.dart
│   │   ├── di/injection.dart                    # get_it + injectable
│   │   ├── di/injection.config.dart
│   │   ├── errors/failures.dart                 # Sealed failure types
│   │   ├── errors/exceptions.dart
│   │   ├── network/network_info.dart
│   │   └── utils/debouncer.dart                 # 400ms debounce utility
│   │
│   ├── languages/
│   │   └── language_registry.dart               # ★ Single source of truth for all languages
│   │
│   ├── features/
│   │   └── translation/
│   │       ├── data/
│   │       │   ├── datasources/
│   │       │   │   ├── translation_remote_datasource.dart
│   │       │   │   └── translation_remote_datasource_impl.dart
│   │       │   ├── models/
│   │       │   │   └── translation_model.dart
│   │       │   └── repositories/
│   │       │       └── translation_repository_impl.dart
│   │       │
│   │       ├── domain/
│   │       │   ├── entities/
│   │       │   │   ├── language.dart
│   │       │   │   └── translation_result.dart
│   │       │   ├── repositories/
│   │       │   │   └── translation_repository.dart
│   │       │   └── usecases/
│   │       │       ├── translate_text.dart
│   │       │       └── detect_language.dart
│   │       │
│   │       └── presentation/
│   │           ├── bloc/
│   │           │   ├── translation_bloc.dart
│   │           │   ├── translation_event.dart
│   │           │   └── translation_state.dart
│   │           └── widgets/
│   │               ├── keyboard_panel.dart       # ★ Root widget of the extension panel
│   │               ├── input_section.dart
│   │               ├── output_section.dart
│   │               ├── language_selector_row.dart
│   │               ├── action_buttons_row.dart
│   │               └── loading_shimmer.dart
│   │
│   └── companion_app/                           # The host app (settings, onboarding, enable instructions)
│       └── pages/
│           ├── home_page.dart
│           └── setup_guide_page.dart
│
├── android/
│   └── app/src/main/
│       ├── kotlin/.../
│       │   ├── MainActivity.kt
│       │   ├── TranslatorInputMethodService.kt  # ★ Android IME service
│       │   └── FlutterKeyboardEngine.kt         # Hosts Flutter engine inside IME
│       └── res/
│           ├── xml/method.xml                   # IME metadata declaration
│           └── layout/keyboard_input_view.xml   # Container for Flutter view
│
├── ios/
│   ├── Runner/
│   │   └── AppDelegate.swift
│   └── TranslatorKeyboard/                      # ★ iOS Keyboard Extension target
│       ├── KeyboardViewController.swift         # UIInputViewController subclass
│       ├── FlutterKeyboardBridge.swift          # Hosts Flutter engine in extension
│       └── Info.plist                           # Extension capabilities declaration
│
└── pubspec.yaml
```

---

## ⚙️ How the Hybrid Architecture Works

### Android Flow

```
User activates keyboard
        │
        ▼
TranslatorInputMethodService (extends InputMethodService)
        │  creates and manages
        ▼
FlutterEngine (cached, singleton)
        │  renders into
        ▼
FlutterView (embedded in IME's onCreateInputView())
        │  shows
        ▼
KeyboardPanel Flutter Widget
        │  on "Inject" tap →
        ▼
currentInputConnection.commitText(translatedText)  ← injects into any active app
```

### iOS Flow

```
User switches to Translator keyboard
        │
        ▼
KeyboardViewController (extends UIInputViewController)
        │  initializes
        ▼
FlutterEngine + FlutterViewController
        │  embedded as child view controller
        ▼
KeyboardPanel Flutter Widget
        │  on "Inject" tap → MethodChannel call →
        ▼
self.textDocumentProxy.insertText(translatedText)  ← injects into any active app
```

---

## 📦 pubspec.yaml

```yaml
name: translator_keyboard
description: Real-time translation keyboard extension

environment:
  sdk: ">=3.3.0 <4.0.0"
  flutter: ">=3.22.0"

dependencies:
  flutter:
    sdk: flutter

  # State Management
  flutter_bloc: ^8.1.6
  equatable: ^2.0.5

  # DI
  get_it: ^7.7.0
  injectable: ^2.4.4

  # Networking
  dio: ^5.4.3+1

  # Functional types
  dartz: ^0.10.1

  # Storage (persist language preferences)
  shared_preferences: ^2.3.1

  # Connectivity
  connectivity_plus: ^6.0.5

  # UI
  google_fonts: ^6.2.1
  flutter_animate: ^4.5.0
  gap: ^3.0.1
  shimmer: ^3.0.0

  # Env / Config
  flutter_dotenv: ^5.1.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  injectable_generator: ^2.6.1
  build_runner: ^2.4.11
  bloc_test: ^9.1.7
  mocktail: ^1.0.4
```

---

## 🌐 Language Registry (Scalability Core)

```dart
// lib/languages/language_registry.dart
// ★ Adding a new language = ONE new entry here. Nothing else changes.

class Language extends Equatable {
  final String code;         // ISO 639-1
  final String name;         // English label
  final String nativeName;   // Label in its own script
  final bool isRTL;
  final String flagEmoji;
  final String fontFamily;   // Google Font to use for this language

  const Language({
    required this.code,
    required this.name,
    required this.nativeName,
    required this.isRTL,
    required this.flagEmoji,
    required this.fontFamily,
  });

  @override
  List<Object> get props => [code];
}

class LanguageRegistry {
  static const List<Language> supported = [
    Language(code: 'en', name: 'English', nativeName: 'English',  isRTL: false, flagEmoji: '🇬🇧', fontFamily: 'Inter'),
    Language(code: 'ar', name: 'Arabic',  nativeName: 'العربية',  isRTL: true,  flagEmoji: '🇸🇦', fontFamily: 'Cairo'),
    Language(code: 'fr', name: 'French',  nativeName: 'Français', isRTL: false, flagEmoji: '🇫🇷', fontFamily: 'Inter'),
    // ✅ Future: Language(code: 'es', name: 'Spanish', nativeName: 'Español', isRTL: false, flagEmoji: '🇪🇸', fontFamily: 'Inter'),
    // ✅ Future: Language(code: 'zh', name: 'Chinese', nativeName: '中文',    isRTL: false, flagEmoji: '🇨🇳', fontFamily: 'Noto Sans SC'),
  ];

  static Language? findByCode(String code) =>
      supported.cast<Language?>().firstWhere((l) => l?.code == code, orElse: () => null);

  static Language get defaultTarget => supported.firstWhere((l) => l.code == 'ar');
}
```

---

## 🤖 Android Native Implementation

### AndroidManifest.xml additions

```xml
<!-- Register the IME service -->
<service
    android:name=".TranslatorInputMethodService"
    android:label="@string/app_name"
    android:permission="android.permission.BIND_INPUT_METHOD"
    android:exported="true">
  <intent-filter>
    <action android:name="android.view.InputMethod" />
  </intent-filter>
  <meta-data
      android:name="android.view.im"
      android:resource="@xml/method" />
</service>
```

### res/xml/method.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity=".MainActivity" />
```

### TranslatorInputMethodService.kt

```kotlin
class TranslatorInputMethodService : InputMethodService() {

    private lateinit var flutterEngine: FlutterEngine
    private lateinit var methodChannel: MethodChannel

    override fun onCreate() {
        super.onCreate()
        initFlutterEngine()
    }

    private fun initFlutterEngine() {
        flutterEngine = FlutterEngine(this).apply {
            dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
                // Note: use a named entrypoint: keyboardMain()
            )
            FlutterEngineCache.getInstance().put("keyboard_engine", this)
        }

        // MethodChannel to receive inject/dismiss commands from Flutter
        methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "translator_keyboard/actions"
        ).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "injectText" -> {
                        val text = call.argument<String>("text") ?: ""
                        currentInputConnection?.commitText(text, 1)
                        result.success(null)
                    }
                    "dismissKeyboard" -> {
                        requestHideSelf(0)
                        result.success(null)
                    }
                    "deleteLastChar" -> {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        // Embed Flutter view inside the IME
        return FlutterView(this, FlutterSurfaceView(this, transparencyMode = TransparencyMode.opaque)).apply {
            attachToFlutterEngine(flutterEngine)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Notify Flutter that the keyboard became active
        methodChannel.invokeMethod("onKeyboardShown", null)
    }

    override fun onDestroy() {
        flutterEngine.destroy()
        super.onDestroy()
    }
}
```

---

## 🍎 iOS Native Implementation

### Create a new Keyboard Extension target in Xcode:

`File → New → Target → Custom Keyboard Extension`

- Product Name: `TranslatorKeyboard`
- Ensure "Requires Full Access" = YES in Info.plist (needed for network calls)

### Info.plist (Extension)

```xml
<key>NSExtension</key>
<dict>
    <key>NSExtensionAttributes</key>
    <dict>
        <key>IsASCIICapable</key><false/>
        <key>PrefersRightToLeft</key><false/>
        <key>PrimaryLanguage</key><string>en-US</string>
        <key>RequestsOpenAccess</key><true/>   <!-- Required for API calls -->
    </dict>
    <key>NSExtensionPointIdentifier</key>
    <string>com.apple.keyboard-service</string>
    <key>NSExtensionPrincipalClass</key>
    <string>$(PRODUCT_MODULE_NAME).KeyboardViewController</string>
</dict>
```

### KeyboardViewController.swift

```swift
import UIKit
import Flutter

class KeyboardViewController: UIInputViewController {

    private var flutterEngine: FlutterEngine!
    private var flutterVC: FlutterViewController!
    private var channel: FlutterMethodChannel!

    override func viewDidLoad() {
        super.viewDidLoad()
        setupFlutterEngine()
        setupMethodChannel()
        embedFlutterView()
    }

    private func setupFlutterEngine() {
        flutterEngine = FlutterEngine(name: "keyboard_engine", project: nil)
        // Use named entrypoint matching keyboard_main.dart
        flutterEngine.run(withEntrypoint: "keyboardMain", libraryURI: nil)
        flutterVC = FlutterViewController(engine: flutterEngine, nibName: nil, bundle: nil)
    }

    private func setupMethodChannel() {
        channel = FlutterMethodChannel(
            name: "translator_keyboard/actions",
            binaryMessenger: flutterEngine.binaryMessenger
        )
        channel.setMethodCallHandler { [weak self] call, result in
            guard let self else { return }
            switch call.method {
            case "injectText":
                if let text = (call.arguments as? [String: Any])?["text"] as? String {
                    self.textDocumentProxy.insertText(text)
                }
                result(nil)
            case "dismissKeyboard":
                self.dismissKeyboard()
                result(nil)
            case "deleteLastChar":
                self.textDocumentProxy.deleteBackward()
                result(nil)
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }

    private func embedFlutterView() {
        addChild(flutterVC)
        view.addSubview(flutterVC.view)
        flutterVC.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            flutterVC.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            flutterVC.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            flutterVC.view.topAnchor.constraint(equalTo: view.topAnchor),
            flutterVC.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        flutterVC.didMove(toParent: self)

        // Set preferred height (keyboard panel height)
        let heightConstraint = view.heightAnchor.constraint(equalToConstant: 260)
        heightConstraint.priority = .required
        heightConstraint.isActive = true
    }
}
```

---

## 🎯 Flutter Keyboard Entry Point

```dart
// lib/keyboard_main.dart

@pragma('vm:entry-point')
void keyboardMain() {
  WidgetsFlutterBinding.ensureInitialized();
  configureDependencies();          // get_it DI setup
  runApp(const KeyboardPanelApp());
}

class KeyboardPanelApp extends StatelessWidget {
  const KeyboardPanelApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: ThemeMode.system,
      home: BlocProvider(
        create: (_) => getIt<TranslationBloc>(),
        child: const KeyboardPanel(),
      ),
    );
  }
}
```

---

## 📡 Flutter ↔ Native Method Channel

```dart
// lib/core/platform/keyboard_channel.dart

class KeyboardChannel {
  static const _channel = MethodChannel('translator_keyboard/actions');

  /// Injects the given text into the active text field in ANY app
  static Future<void> injectText(String text) async {
    await _channel.invokeMethod('injectText', {'text': text});
  }

  /// Dismisses the keyboard
  static Future<void> dismiss() async {
    await _channel.invokeMethod('dismissKeyboard');
  }

  /// Deletes the last character (backspace)
  static Future<void> deleteLastChar() async {
    await _channel.invokeMethod('deleteLastChar');
  }
}
```

---

## 🎨 KeyboardPanel UI Widget

```dart
// lib/features/translation/presentation/widgets/keyboard_panel.dart

class KeyboardPanel extends StatelessWidget {
  const KeyboardPanel({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<TranslationBloc, TranslationState>(
      builder: (context, state) {
        return Container(
          height: 260,
          color: Theme.of(context).colorScheme.surface,
          child: Column(
            children: [
              // ── Row 1: Language Selector ──────────────────────
              LanguageSelectorRow(
                sourceLanguage: state.detectedSourceLanguage,
                targetLanguage: state.targetLanguage,
                onTargetChanged: (lang) => context
                    .read<TranslationBloc>()
                    .add(TargetLanguageChanged(lang)),
                onSwap: () => context
                    .read<TranslationBloc>()
                    .add(SwapLanguagesRequested()),
              ),

              const Divider(height: 1),

              // ── Row 2: Input Field ────────────────────────────
              Expanded(
                child: InputSection(
                  text: state.inputText,
                  isRTL: state.isInputRTL,
                  fontFamily: state.detectedSourceLanguage?.fontFamily,
                  onChanged: (text) => context
                      .read<TranslationBloc>()
                      .add(TextChangedEvent(text)),
                ),
              ),

              const Divider(height: 1),

              // ── Row 3: Output / Translation ───────────────────
              Expanded(
                child: state.status == TranslationStatus.loading
                    ? const LoadingShimmer()
                    : OutputSection(
                        text: state.outputText,
                        isRTL: state.isOutputRTL,
                        fontFamily: state.targetLanguage.fontFamily,
                        errorMessage: state.errorMessage,
                      ),
              ),

              const Divider(height: 1),

              // ── Row 4: Action Buttons ─────────────────────────
              ActionButtonsRow(
                onSendTranslated: () async {
                  await KeyboardChannel.injectText(state.outputText);
                },
                onSendOriginal: () async {
                  await KeyboardChannel.injectText(state.inputText);
                },
                onClear: () => context
                    .read<TranslationBloc>()
                    .add(ClearTextRequested()),
                canSend: state.outputText.isNotEmpty,
              ),
            ],
          ),
        );
      },
    );
  }
}
```

---

## 🔄 BLoC Design

### Events

```dart
abstract class TranslationEvent extends Equatable {}

class TextChangedEvent        extends TranslationEvent { final String text; }
class TargetLanguageChanged   extends TranslationEvent { final Language language; }
class SwapLanguagesRequested  extends TranslationEvent {}
class ClearTextRequested      extends TranslationEvent {}
```

### State

```dart
enum TranslationStatus { idle, loading, success, failure }

class TranslationState extends Equatable {
  final String inputText;
  final String outputText;
  final Language? detectedSourceLanguage;
  final Language targetLanguage;
  final TranslationStatus status;
  final String? errorMessage;

  bool get isInputRTL  => detectedSourceLanguage?.isRTL ?? false;
  bool get isOutputRTL => targetLanguage.isRTL;

  // copyWith(...) required
}
```

### BLoC Core Logic

```dart
class TranslationBloc extends Bloc<TranslationEvent, TranslationState> {
  final TranslateText _translateText;
  final DetectLanguage _detectLanguage;
  final Debouncer _debouncer;

  TranslationBloc({...}) : super(TranslationState.initial()) {

    on<TextChangedEvent>((event, emit) {
      emit(state.copyWith(inputText: event.text));
      if (event.text.trim().length < 2) return;

      _debouncer.run(() {
        add(_TriggerTranslation(event.text));   // internal event
      });
    });

    on<_TriggerTranslation>((event, emit) async {
      emit(state.copyWith(status: TranslationStatus.loading));

      // 1. Auto-detect source language
      final detected = await _detectLanguage(event.text);
      final sourceLang = detected.fold(
        (_) => null,
        (code) => LanguageRegistry.findByCode(code),
      );

      // 2. Guard: if source == target, auto-pick next language
      final target = sourceLang?.code == state.targetLanguage.code
          ? LanguageRegistry.supported.firstWhere((l) => l.code != sourceLang!.code)
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
    });

    on<SwapLanguagesRequested>((event, emit) {
      if (state.outputText.isEmpty || state.detectedSourceLanguage == null) return;
      emit(state.copyWith(
        inputText: state.outputText,
        outputText: state.inputText,
        targetLanguage: state.detectedSourceLanguage!,
        detectedSourceLanguage: state.targetLanguage,
      ));
      add(_TriggerTranslation(state.outputText));
    });

    on<ClearTextRequested>((event, emit) {
      emit(TranslationState.initial());
    });
  }
}
```

---

## 🌍 Translation API Layer

Use **MyMemory API** (free, no key required for basic use) or **LibreTranslate**:

```dart
// translation_remote_datasource_impl.dart

@Injectable(as: TranslationRemoteDataSource)
class TranslationRemoteDataSourceImpl implements TranslationRemoteDataSource {
  final Dio _dio;

  @override
  Future<TranslationModel> translateText({
    required String text,
    required String sourceLangCode,
    required String targetLangCode,
  }) async {
    final response = await _dio.get(
      'https://api.mymemory.translated.net/get',
      queryParameters: {
        'q': text,
        'langpair': '$sourceLangCode|$targetLangCode',
      },
    );
    return TranslationModel.fromJson(response.data);
  }

  @override
  Future<String> detectLanguage(String text) async {
    // Use LibreTranslate /detect endpoint or a lightweight detection library
    final response = await _dio.post(
      'https://libretranslate.com/detect',
      data: {'q': text},
    );
    final List detections = response.data as List;
    return detections.first['language'] as String;
  }
}
```

> **Architecture note:** The data source is hidden behind an abstract interface. Swap to Google Cloud Translation or DeepL by only changing this `_impl.dart` file.

---

## 📱 Companion App (Settings & Onboarding)

The companion app (normal Flutter app) serves one purpose: **guide the user to enable the keyboard extension in device settings**.

### Android Setup Guide Page content:

1. Go to **Settings → General Management → Keyboard → On-screen keyboard**
2. Toggle on **Translator Keyboard**
3. Grant all requested permissions
4. Open any app and long-press the keyboard icon to switch

### iOS Setup Guide Page content:

1. Go to **Settings → General → Keyboard → Keyboards → Add New Keyboard**
2. Select **Translator Keyboard**
3. Tap the globe icon while typing in any app to switch

---

## ✅ Implementation Checklist

### Phase 1 — Native Shell

- [ ] Android: create `TranslatorInputMethodService`, declare in Manifest, `method.xml`
- [ ] iOS: create Keyboard Extension target, configure `Info.plist` with Full Access
- [ ] Both: implement `MethodChannel` for `injectText`, `dismiss`, `deleteLastChar`
- [ ] Flutter: create `keyboardMain()` named entry point
- [ ] Verify Flutter view renders inside native keyboard container on both platforms

### Phase 2 — Language & Data Layer

- [ ] Implement `LanguageRegistry` with EN / AR / FR
- [ ] Implement `TranslationRemoteDataSource` (MyMemory API)
- [ ] Implement `DetectLanguage` data source (LibreTranslate /detect)
- [ ] Map errors → `Failure` sealed classes via `Either<Failure, T>`

### Phase 3 — BLoC & Domain

- [ ] `TranslateText` use case
- [ ] `DetectLanguage` use case
- [ ] `TranslationBloc` with debounce (400ms), auto-detect, same-language guard, swap

### Phase 4 — Flutter UI (KeyboardPanel)

- [ ] `LanguageSelectorRow` — shows detected source (read-only chip) + target dropdown
- [ ] `InputSection` — RTL/LTR aware `TextField`, no system keyboard (keyboard type = none on panel)
- [ ] `OutputSection` — `SelectableText`, RTL/LTR aware, shimmer loading
- [ ] `ActionButtonsRow` — **Send Translated**, **Send Original**, **Clear**, **⌫ Backspace**
- [ ] Dynamic font switching per language (Cairo for Arabic, Inter for Latin)
- [ ] Light + dark theme support

### Phase 5 — Companion App

- [ ] Home page with keyboard activation status
- [ ] Step-by-step setup guide (Android & iOS)
- [ ] Language preferences screen (persist with `SharedPreferences`)

### Phase 6 — QA & Polish

- [ ] Test text injection in WhatsApp, Telegram, iMessage, Gmail
- [ ] Test RTL/LTR switching mid-conversation
- [ ] Test with very long text (>500 chars)
- [ ] Test offline / network failure graceful error
- [ ] Unit tests: BLoC events, use cases, repository (mocktail)

---

## ⚠️ Critical Implementation Notes

1. **No system keyboard inside the panel**: The Flutter `TextField` inside the keyboard panel must NOT trigger the system keyboard. Set `readOnly: false` but intercept input via custom key handlers, OR use the native keyboard below and only use the Flutter panel for input composition and translation display.

2. **iOS Full Access required**: Network calls from a keyboard extension require `RequestsOpenAccess = true` in Info.plist. Without it, all HTTP calls will silently fail. Show a clear warning in the companion app if Full Access is not granted.

3. **Panel height**: Keep the total panel height ≤ 270px on Android, ≤ 260px on iOS to avoid covering content. Use a fixed `HeightConstraint` on iOS, override `onMeasure` on Android.

4. **Flutter Engine lifecycle**: Use `FlutterEngineCache` on Android and a retained `FlutterEngine` instance on iOS. Do NOT create a new engine per keyboard show/hide — it causes severe lag.

5. **Text injection timing**: On Android, always call `commitText` on the main thread via `Handler(Looper.getMainLooper()).post { ... }`. On iOS, `textDocumentProxy.insertText` is already on the main thread.

6. **Adding a future language**: Only touch `LanguageRegistry.supported`. All UI selectors, font switching, RTL detection, and API language codes are driven from that single list automatically.
