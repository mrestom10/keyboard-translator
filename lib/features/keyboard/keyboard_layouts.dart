import 'keyboard_key_type.dart';

class KeyboardLayouts {
  KeyboardLayouts._();

  static const List<List<KeyboardKeyData>> qwerty = [
    // Row 1
    [
      KeyboardKeyData(label: 'q'),
      KeyboardKeyData(label: 'w'),
      KeyboardKeyData(label: 'e'),
      KeyboardKeyData(label: 'r'),
      KeyboardKeyData(label: 't'),
      KeyboardKeyData(label: 'y'),
      KeyboardKeyData(label: 'u'),
      KeyboardKeyData(label: 'i'),
      KeyboardKeyData(label: 'o'),
      KeyboardKeyData(label: 'p'),
    ],
    // Row 2
    [
      KeyboardKeyData(label: 'a'),
      KeyboardKeyData(label: 's'),
      KeyboardKeyData(label: 'd'),
      KeyboardKeyData(label: 'f'),
      KeyboardKeyData(label: 'g'),
      KeyboardKeyData(label: 'h'),
      KeyboardKeyData(label: 'j'),
      KeyboardKeyData(label: 'k'),
      KeyboardKeyData(label: 'l'),
    ],
    // Row 3
    [
      KeyboardKeyData(
        label: '\u21E7',
        type: KeyboardKeyType.shift,
        widthFactor: 1.5,
      ),
      KeyboardKeyData(label: 'z'),
      KeyboardKeyData(label: 'x'),
      KeyboardKeyData(label: 'c'),
      KeyboardKeyData(label: 'v'),
      KeyboardKeyData(label: 'b'),
      KeyboardKeyData(label: 'n'),
      KeyboardKeyData(label: 'm'),
      KeyboardKeyData(
        label: '\u232B',
        type: KeyboardKeyType.backspace,
        widthFactor: 1.5,
      ),
    ],
    // Row 4
    [
      KeyboardKeyData(
        label: '123',
        type: KeyboardKeyType.switchToNumbers,
        widthFactor: 1.5,
      ),
      KeyboardKeyData(label: ','),
      KeyboardKeyData(
        label: ' ',
        type: KeyboardKeyType.space,
        widthFactor: 4.0,
      ),
      KeyboardKeyData(label: '.'),
      KeyboardKeyData(
        label: '\u23CE',
        type: KeyboardKeyType.enter,
        widthFactor: 1.5,
      ),
    ],
  ];

  static const List<List<KeyboardKeyData>> numbers = [
    // Row 1
    [
      KeyboardKeyData(label: '1'),
      KeyboardKeyData(label: '2'),
      KeyboardKeyData(label: '3'),
      KeyboardKeyData(label: '4'),
      KeyboardKeyData(label: '5'),
      KeyboardKeyData(label: '6'),
      KeyboardKeyData(label: '7'),
      KeyboardKeyData(label: '8'),
      KeyboardKeyData(label: '9'),
      KeyboardKeyData(label: '0'),
    ],
    // Row 2
    [
      KeyboardKeyData(label: '@'),
      KeyboardKeyData(label: '#'),
      KeyboardKeyData(label: '\$'),
      KeyboardKeyData(label: '%'),
      KeyboardKeyData(label: '&'),
      KeyboardKeyData(label: '-'),
      KeyboardKeyData(label: '+'),
      KeyboardKeyData(label: '('),
      KeyboardKeyData(label: ')'),
    ],
    // Row 3
    [
      KeyboardKeyData(
        label: '#+=',
        type: KeyboardKeyType.switchToSymbols,
        widthFactor: 1.5,
      ),
      KeyboardKeyData(label: '*'),
      KeyboardKeyData(label: '"'),
      KeyboardKeyData(label: "'"),
      KeyboardKeyData(label: ':'),
      KeyboardKeyData(label: ';'),
      KeyboardKeyData(label: '!'),
      KeyboardKeyData(label: '?'),
      KeyboardKeyData(
        label: '\u232B',
        type: KeyboardKeyType.backspace,
        widthFactor: 1.5,
      ),
    ],
    // Row 4
    [
      KeyboardKeyData(
        label: 'ABC',
        type: KeyboardKeyType.switchToLetters,
        widthFactor: 1.5,
      ),
      KeyboardKeyData(label: ','),
      KeyboardKeyData(
        label: ' ',
        type: KeyboardKeyType.space,
        widthFactor: 4.0,
      ),
      KeyboardKeyData(label: '.'),
      KeyboardKeyData(
        label: '\u23CE',
        type: KeyboardKeyType.enter,
        widthFactor: 1.5,
      ),
    ],
  ];

  static const List<List<KeyboardKeyData>> symbols = [
    // Row 1
    [
      KeyboardKeyData(label: '~'),
      KeyboardKeyData(label: '`'),
      KeyboardKeyData(label: '|'),
      KeyboardKeyData(label: '\u2022'),
      KeyboardKeyData(label: '\u221A'),
      KeyboardKeyData(label: '\u03C0'),
      KeyboardKeyData(label: '\u00F7'),
      KeyboardKeyData(label: '\u00D7'),
      KeyboardKeyData(label: '{'),
      KeyboardKeyData(label: '}'),
    ],
    // Row 2
    [
      KeyboardKeyData(label: '\u00A3'),
      KeyboardKeyData(label: '\u00A2'),
      KeyboardKeyData(label: '\u20AC'),
      KeyboardKeyData(label: '\u00A5'),
      KeyboardKeyData(label: '^'),
      KeyboardKeyData(label: '\u00B0'),
      KeyboardKeyData(label: '='),
      KeyboardKeyData(label: '['),
      KeyboardKeyData(label: ']'),
    ],
    // Row 3
    [
      KeyboardKeyData(
        label: '123',
        type: KeyboardKeyType.switchToNumbers,
        widthFactor: 1.5,
      ),
      KeyboardKeyData(label: '\\'),
      KeyboardKeyData(label: '/'),
      KeyboardKeyData(label: '_'),
      KeyboardKeyData(label: '<'),
      KeyboardKeyData(label: '>'),
      KeyboardKeyData(label: '\u2026'),
      KeyboardKeyData(label: '\u00BF'),
      KeyboardKeyData(
        label: '\u232B',
        type: KeyboardKeyType.backspace,
        widthFactor: 1.5,
      ),
    ],
    // Row 4
    [
      KeyboardKeyData(
        label: 'ABC',
        type: KeyboardKeyType.switchToLetters,
        widthFactor: 1.5,
      ),
      KeyboardKeyData(label: ','),
      KeyboardKeyData(
        label: ' ',
        type: KeyboardKeyType.space,
        widthFactor: 4.0,
      ),
      KeyboardKeyData(label: '.'),
      KeyboardKeyData(
        label: '\u23CE',
        type: KeyboardKeyType.enter,
        widthFactor: 1.5,
      ),
    ],
  ];
}
