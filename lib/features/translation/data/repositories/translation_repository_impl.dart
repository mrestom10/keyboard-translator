import 'package:dartz/dartz.dart';
import 'package:translator_keyboard/core/errors/exceptions.dart';
import 'package:translator_keyboard/core/errors/failures.dart';
import 'package:translator_keyboard/core/network/network_info.dart';
import 'package:translator_keyboard/features/translation/data/datasources/translation_remote_datasource.dart';
import 'package:translator_keyboard/features/translation/domain/entities/translation_result.dart';
import 'package:translator_keyboard/features/translation/domain/repositories/translation_repository.dart';

class TranslationRepositoryImpl implements TranslationRepository {
  final TranslationRemoteDataSource remoteDataSource;
  final NetworkInfo networkInfo;

  TranslationRepositoryImpl({
    required this.remoteDataSource,
    required this.networkInfo,
  });

  @override
  Future<Either<Failure, TranslationResult>> translateText({
    required String text,
    required String sourceLangCode,
    required String targetLangCode,
  }) async {
    if (!await networkInfo.isConnected) {
      return const Left(NetworkFailure());
    }

    try {
      final result = await remoteDataSource.translateText(
        text: text,
        sourceLangCode: sourceLangCode,
        targetLangCode: targetLangCode,
      );
      return Right(result);
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    }
  }

  @override
  Future<Either<Failure, String>> detectLanguage(String text) async {
    if (!await networkInfo.isConnected) {
      return const Left(NetworkFailure());
    }

    try {
      final langCode = await remoteDataSource.detectLanguage(text);
      return Right(langCode);
    } on ServerException catch (e) {
      return Left(DetectionFailure(e.message));
    }
  }
}
