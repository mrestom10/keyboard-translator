import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:translator_keyboard/core/constants/app_constants.dart';
import 'package:translator_keyboard/core/platform/keyboard_channel.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_bloc.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_event.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_state.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/action_buttons_row.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/input_section.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/language_selector_row.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/loading_shimmer.dart';
import 'package:translator_keyboard/features/translation/presentation/widgets/output_section.dart';

class KeyboardPanel extends StatelessWidget {
  const KeyboardPanel({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<TranslationBloc, TranslationState>(
      builder: (context, state) {
        return Container(
          height: AppConstants.keyboardPanelHeight,
          color: Theme.of(context).colorScheme.surface,
          child: Column(
            children: [
              // Row 1: Language Selector
              LanguageSelectorRow(
                sourceLanguage: state.detectedSourceLanguage,
                targetLanguage: state.targetLanguage,
                onTargetChanged: (lang) => context
                    .read<TranslationBloc>()
                    .add(TargetLanguageChanged(lang)),
                onSwap: () => context
                    .read<TranslationBloc>()
                    .add(const SwapLanguagesRequested()),
              ),

              const Divider(height: 1),

              // Row 2: Input Field
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

              // Row 3: Output / Translation
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

              // Row 4: Action Buttons
              ActionButtonsRow(
                onSendTranslated: () async {
                  await KeyboardChannel.injectText(state.outputText);
                },
                onSendOriginal: () async {
                  await KeyboardChannel.injectText(state.inputText);
                },
                onClear: () => context
                    .read<TranslationBloc>()
                    .add(const ClearTextRequested()),
                canSend: state.outputText.isNotEmpty,
              ),
            ],
          ),
        );
      },
    );
  }
}
