import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:gap/gap.dart';
import 'package:translator_keyboard/core/platform/keyboard_channel.dart';
import 'package:translator_keyboard/features/keyboard/custom_keyboard.dart';
import 'package:translator_keyboard/features/keyboard/translation_bar.dart';
import 'package:translator_keyboard/features/translation/domain/entities/language.dart';
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
  // Buffer for text typed via the custom keyboard keys
  String _typedText = '';

  void _onTextInput(String char) {
    setState(() {
      _typedText += char;
    });
    // Inject character into the host app's text field
    KeyboardChannel.injectText(char);
    // Trigger translation
    context.read<TranslationBloc>().add(TextChangedEvent(_typedText));
  }

  void _onBackspace() {
    if (_typedText.isNotEmpty) {
      setState(() {
        _typedText = _typedText.substring(0, _typedText.length - 1);
      });
    }
    // Delete from host app
    KeyboardChannel.deleteLastChar();
    // Update translation
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

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return BlocBuilder<TranslationBloc, TranslationState>(
      builder: (context, state) {
        return Container(
          color: colorScheme.surfaceContainerLow,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Translation bar (compact, above keyboard)
              TranslationBar(
                inputText: _typedText,
                outputText: state.outputText,
                isLoading: state.status == TranslationStatus.loading,
                errorMessage: state.errorMessage,
                sourceLangFlag: state.detectedSourceLanguage?.flagEmoji ?? '',
                sourceLangName:
                    state.detectedSourceLanguage?.name ?? 'Auto-detect',
                targetLangFlag: state.targetLanguage.flagEmoji,
                targetLangName: state.targetLanguage.name,
                onSendTranslated: () async {
                  if (state.outputText.isNotEmpty) {
                    // Delete what was typed, inject translation instead
                    for (int i = 0; i < _typedText.length; i++) {
                      await KeyboardChannel.deleteLastChar();
                    }
                    await KeyboardChannel.injectText(state.outputText);
                    _onClear();
                  }
                },
                onSendOriginal: () {
                  // Original text is already typed in, just clear state
                  _onClear();
                },
                onClear: _onClear,
                onSwapLanguages: () => context
                    .read<TranslationBloc>()
                    .add(const SwapLanguagesRequested()),
                languageDropdown: _buildTargetDropdown(context, state),
              ),

              // Custom keyboard
              CustomKeyboard(
                onTextInput: _onTextInput,
                onBackspace: _onBackspace,
                onEnter: _onEnter,
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildTargetDropdown(BuildContext context, TranslationState state) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 6),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(6),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<Language>(
          value: state.targetLanguage,
          isExpanded: true,
          isDense: true,
          icon: Icon(Icons.arrow_drop_down,
              color: colorScheme.onPrimaryContainer, size: 16),
          items: LanguageRegistry.supported.map((lang) {
            return DropdownMenuItem<Language>(
              value: lang,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(lang.flagEmoji, style: const TextStyle(fontSize: 14)),
                  const Gap(4),
                  Flexible(
                    child: Text(
                      lang.name,
                      style: TextStyle(
                        fontSize: 11,
                        color: colorScheme.onPrimaryContainer,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
          onChanged: (lang) {
            if (lang != null) {
              context
                  .read<TranslationBloc>()
                  .add(TargetLanguageChanged(lang));
            }
          },
        ),
      ),
    );
  }
}
