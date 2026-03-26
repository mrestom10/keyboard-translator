enum KeyboardKeyType {
  character,
  backspace,
  shift,
  space,
  enter,
  switchToNumbers,
  switchToLetters,
  switchToSymbols,
}

class KeyboardKeyData {
  final String label;
  final String? shiftLabel;
  final KeyboardKeyType type;
  final double widthFactor;

  const KeyboardKeyData({
    required this.label,
    this.shiftLabel,
    this.type = KeyboardKeyType.character,
    this.widthFactor = 1.0,
  });

  String displayLabel(bool isShifted) {
    if (type == KeyboardKeyType.character) {
      return isShifted ? (shiftLabel ?? label.toUpperCase()) : label;
    }
    return label;
  }
}
