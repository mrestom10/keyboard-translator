import 'package:flutter/widgets.dart';

/// Entry point stub for the keyboard extension.
/// Translation is now handled entirely in native Kotlin (no Flutter dependency).
/// This entry point exists only because the Android manifest references it.
@pragma('vm:entry-point')
void keyboardMain() {
  // No-op: keyboard is 100% native now.
  debugPrint('KEYBOARD: Native keyboard active, no Flutter rendering needed');
}
