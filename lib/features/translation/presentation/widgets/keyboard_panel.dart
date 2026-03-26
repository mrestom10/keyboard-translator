import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:translator_keyboard/core/platform/keyboard_channel.dart';
import 'package:translator_keyboard/features/keyboard/custom_keyboard.dart';
import 'package:translator_keyboard/features/keyboard/translation_bar.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_bloc.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_event.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_state.dart';
import 'package:translator_keyboard/languages/language_registry.dart';

class KeyboardPanel extends StatefulWidget {
  const KeyboardPanel({super.key});

  @override
  State<KeyboardPanel> createState() => _KeyboardPanelState();
}

class _KeyboardPanelState extends State<KeyboardPanel> {
  String _typedText = '';
  int _targetLangIndex = LanguageRegistry.supported
      .indexWhere((l) => l.code == LanguageRegistry.defaultTarget.code);

  void _onTextInput(String char) {
    setState(() {
      _typedText += char;
    });
    KeyboardChannel.injectText(char);
    context.read<TranslationBloc>().add(TextChangedEvent(_typedText));
  }

  void _onBackspace() {
    if (_typedText.isNotEmpty) {
      setState(() {
        _typedText = _typedText.substring(0, _typedText.length - 1);
      });
    }
    KeyboardChannel.deleteLastChar();
    context.read<TranslationBloc>().add(TextChangedEvent(_typedText));
  }

  void _onEnter() {
    KeyboardChannel.injectText('\n');
  }

  void _onClear() {
    setState(() {
      _typedText = '';
    });
    context.read<TranslationBloc>().add(const ClearTextRequested());
  }

  void _cycleTargetLanguage() {
    setState(() {
      _targetLangIndex =
          (_targetLangIndex + 1) % LanguageRegistry.supported.length;
    });
    final lang = LanguageRegistry.supported[_targetLangIndex];
    context.read<TranslationBloc>().add(TargetLanguageChanged(lang));
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return BlocBuilder<TranslationBloc, TranslationState>(
      builder: (context, state) {
        return SizedBox.expand(
          child: Container(
            color: colorScheme.surfaceContainerLow,
            child: Column(
              children: [
                // Translation bar (compact, above keyboard)
                TranslationBar(
                  inputText: _typedText,
                  outputText: state.outputText,
                  isLoading: state.status == TranslationStatus.loading,
                  errorMessage: state.errorMessage,
                  sourceLangFlag:
                      state.detectedSourceLanguage?.flagEmoji ?? '',
                  sourceLangName:
                      state.detectedSourceLanguage?.name ?? 'Auto',
                  targetLangFlag: state.targetLanguage.flagEmoji,
                  targetLangName: state.targetLanguage.name,
                  onSendTranslated: () async {
                    if (state.outputText.isNotEmpty) {
                      for (int i = 0; i < _typedText.length; i++) {
                        await KeyboardChannel.deleteLastChar();
                      }
                      await KeyboardChannel.injectText(state.outputText);
                      _onClear();
                    }
                  },
                  onSendOriginal: _onClear,
                  onClear: _onClear,
                  onSwapLanguages: () => context
                      .read<TranslationBloc>()
                      .add(const SwapLanguagesRequested()),
                  onCycleTargetLanguage: _cycleTargetLanguage,
                ),

                // Custom keyboard fills remaining space
                Expanded(
                  child: CustomKeyboard(
                    onTextInput: _onTextInput,
                    onBackspace: _onBackspace,
                    onEnter: _onEnter,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
