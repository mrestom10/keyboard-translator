import 'package:flutter/services.dart';
import 'package:translator_keyboard/core/constants/app_constants.dart';

class KeyboardChannel {
  static const _channel = MethodChannel(AppConstants.methodChannelName);

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
