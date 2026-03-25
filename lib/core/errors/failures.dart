import 'package:equatable/equatable.dart';

sealed class Failure extends Equatable {
  final String message;

  const Failure(this.message);

  @override
  List<Object> get props => [message];
}

class ServerFailure extends Failure {
  const ServerFailure([super.message = 'Server error occurred']);
}

class NetworkFailure extends Failure {
  const NetworkFailure([super.message = 'No internet connection']);
}

class TranslationFailure extends Failure {
  const TranslationFailure([super.message = 'Translation failed']);
}

class DetectionFailure extends Failure {
  const DetectionFailure([super.message = 'Language detection failed']);
}
