import 'package:flutter/material.dart';
import 'keyboard_key_type.dart';
import 'keyboard_key_widget.dart';
import 'keyboard_layouts.dart';

enum _LayoutMode { letters, numbers, symbols }

class CustomKeyboard extends StatefulWidget {
  final ValueChanged<String> onTextInput;
  final VoidCallback onBackspace;
  final VoidCallback onEnter;

  const CustomKeyboard({
    super.key,
    required this.onTextInput,
    required this.onBackspace,
    required this.onEnter,
  });

  @override
  State<CustomKeyboard> createState() => _CustomKeyboardState();
}

class _CustomKeyboardState extends State<CustomKeyboard> {
  _LayoutMode _layoutMode = _LayoutMode.letters;
  bool _isShifted = false;

  List<List<KeyboardKeyData>> get _currentLayout {
    switch (_layoutMode) {
      case _LayoutMode.letters:
        return KeyboardLayouts.qwerty;
      case _LayoutMode.numbers:
        return KeyboardLayouts.numbers;
      case _LayoutMode.symbols:
        return KeyboardLayouts.symbols;
    }
  }

  void _handleKeyTap(KeyboardKeyData key) {
    switch (key.type) {
      case KeyboardKeyType.character:
        final char = key.displayLabel(_isShifted);
        widget.onTextInput(char);
        if (_isShifted) {
          setState(() => _isShifted = false);
        }
      case KeyboardKeyType.space:
        widget.onTextInput(' ');
      case KeyboardKeyType.backspace:
        widget.onBackspace();
      case KeyboardKeyType.enter:
        widget.onEnter();
      case KeyboardKeyType.shift:
        setState(() => _isShifted = !_isShifted);
      case KeyboardKeyType.switchToNumbers:
        setState(() {
          _layoutMode = _LayoutMode.numbers;
          _isShifted = false;
        });
      case KeyboardKeyType.switchToLetters:
        setState(() {
          _layoutMode = _LayoutMode.letters;
          _isShifted = false;
        });
      case KeyboardKeyType.switchToSymbols:
        setState(() {
          _layoutMode = _LayoutMode.symbols;
          _isShifted = false;
        });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.surfaceContainerLow,
      padding: const EdgeInsets.only(bottom: 4, top: 2),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: _currentLayout
            .map((row) => _buildRow(context, row))
            .toList(),
      ),
    );
  }

  Widget _buildRow(BuildContext context, List<KeyboardKeyData> row) {
    final screenWidth = MediaQuery.of(context).size.width;
    const totalPadding = 8.0; // horizontal padding
    final availableWidth = screenWidth - totalPadding;

    // Calculate total weight for this row
    final totalWeight = row.fold<double>(0, (sum, k) => sum + k.widthFactor);
    final unitWidth = availableWidth / totalWeight;
    const keyHeight = 42.0;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: row.map((keyData) {
          final keyWidth = unitWidth * keyData.widthFactor - 4; // minus margin
          return KeyboardKeyWidget(
            keyData: keyData,
            isShifted: _isShifted,
            keyWidth: keyWidth,
            keyHeight: keyHeight,
            onTap: () => _handleKeyTap(keyData),
          );
        }).toList(),
      ),
    );
  }
}
