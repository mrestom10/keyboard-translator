import 'package:equatable/equatable.dart';

class Language extends Equatable {
  final String code; // ISO 639-1
  final String name; // English label
  final String nativeName; // Label in its own script
  final bool isRTL;
  final String flagEmoji;
  final String fontFamily; // Google Font to use for this language

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
