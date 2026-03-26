import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'keyboard_key_type.dart';

class KeyboardKeyWidget extends StatefulWidget {
  final KeyboardKeyData keyData;
  final bool isShifted;
  final double keyWidth;
  final double keyHeight;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const KeyboardKeyWidget({
    super.key,
    required this.keyData,
    required this.isShifted,
    required this.keyWidth,
    required this.keyHeight,
    required this.onTap,
    this.onLongPress,
  });

  @override
  State<KeyboardKeyWidget> createState() => _KeyboardKeyWidgetState();
}

class _KeyboardKeyWidgetState extends State<KeyboardKeyWidget> {
  bool _isPressed = false;
  Timer? _repeatTimer;

  void _startRepeat() {
    if (widget.keyData.type == KeyboardKeyType.backspace) {
      _repeatTimer = Timer.periodic(const Duration(milliseconds: 80), (_) {
        widget.onTap();
        HapticFeedback.lightImpact();
      });
    }
  }

  void _stopRepeat() {
    _repeatTimer?.cancel();
    _repeatTimer = null;
  }

  @override
  void dispose() {
    _stopRepeat();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final type = widget.keyData.type;

    Color bgColor;
    Color textColor;
    double fontSize;
    IconData? icon;

    switch (type) {
      case KeyboardKeyType.character:
        bgColor = _isPressed
            ? colorScheme.primaryContainer
            : colorScheme.surfaceContainerHigh;
        textColor = colorScheme.onSurface;
        fontSize = 18;
      case KeyboardKeyType.space:
        bgColor = _isPressed
            ? colorScheme.primaryContainer
            : colorScheme.surfaceContainerHigh;
        textColor = colorScheme.onSurfaceVariant;
        fontSize = 14;
      case KeyboardKeyType.backspace:
        bgColor = _isPressed
            ? colorScheme.errorContainer
            : colorScheme.surfaceContainerHighest;
        textColor = colorScheme.onSurfaceVariant;
        fontSize = 18;
        icon = Icons.backspace_outlined;
      case KeyboardKeyType.shift:
        bgColor = widget.isShifted
            ? colorScheme.primaryContainer
            : colorScheme.surfaceContainerHighest;
        textColor = widget.isShifted
            ? colorScheme.primary
            : colorScheme.onSurfaceVariant;
        fontSize = 18;
        icon = widget.isShifted
            ? Icons.keyboard_capslock
            : Icons.keyboard_arrow_up;
      case KeyboardKeyType.enter:
        bgColor = _isPressed
            ? colorScheme.primary.withValues(alpha: 0.8)
            : colorScheme.primary;
        textColor = colorScheme.onPrimary;
        fontSize = 18;
        icon = Icons.keyboard_return;
      case KeyboardKeyType.switchToNumbers:
      case KeyboardKeyType.switchToLetters:
      case KeyboardKeyType.switchToSymbols:
        bgColor = _isPressed
            ? colorScheme.primaryContainer
            : colorScheme.surfaceContainerHighest;
        textColor = colorScheme.onSurfaceVariant;
        fontSize = 13;
    }

    return GestureDetector(
      onTapDown: (_) {
        setState(() => _isPressed = true);
        HapticFeedback.lightImpact();
      },
      onTapUp: (_) {
        setState(() => _isPressed = false);
        _stopRepeat();
        widget.onTap();
      },
      onTapCancel: () {
        setState(() => _isPressed = false);
        _stopRepeat();
      },
      onLongPressStart: (_) {
        _startRepeat();
        widget.onLongPress?.call();
      },
      onLongPressEnd: (_) {
        setState(() => _isPressed = false);
        _stopRepeat();
      },
      child: Container(
        width: widget.keyWidth,
        height: widget.keyHeight,
        margin: const EdgeInsets.all(2),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(6),
          boxShadow: _isPressed
              ? null
              : [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.15),
                    offset: const Offset(0, 1),
                    blurRadius: 0.5,
                  ),
                ],
        ),
        child: Center(
          child: icon != null
              ? Icon(icon, color: textColor, size: 20)
              : Text(
                  type == KeyboardKeyType.space
                      ? 'space'
                      : widget.keyData.displayLabel(widget.isShifted),
                  style: TextStyle(
                    color: textColor,
                    fontSize: fontSize,
                    fontWeight: type == KeyboardKeyType.character
                        ? FontWeight.w400
                        : FontWeight.w500,
                  ),
                ),
        ),
      ),
    );
  }
}
