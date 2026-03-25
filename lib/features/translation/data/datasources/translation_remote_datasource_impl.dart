import 'package:dio/dio.dart';
import 'package:translator_keyboard/core/constants/app_constants.dart';
import 'package:translator_keyboard/core/errors/exceptions.dart';
import 'package:translator_keyboard/features/translation/data/datasources/translation_remote_datasource.dart';
import 'package:translator_keyboard/features/translation/data/models/translation_model.dart';

class TranslationRemoteDataSourceImpl implements TranslationRemoteDataSource {
  final Dio _dio;

  TranslationRemoteDataSourceImpl(this._dio);

  @override
  Future<TranslationModel> translateText({
    required String text,
    required String sourceLangCode,
    required String targetLangCode,
  }) async {
    try {
      final response = await _dio.get(
        AppConstants.myMemoryBaseUrl,
        queryParameters: {
          'q': text,
          'langpair': '$sourceLangCode|$targetLangCode',
        },
      );

      if (response.statusCode == 200) {
        return TranslationModel.fromMyMemoryJson(
          response.data as Map<String, dynamic>,
          sourceLangCode: sourceLangCode,
          targetLangCode: targetLangCode,
        );
      }

      throw ServerException(
        message: 'Translation failed',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      throw ServerException(
        message: e.message ?? 'Network error during translation',
        statusCode: e.response?.statusCode,
      );
    }
  }

  @override
  Future<String> detectLanguage(String text) async {
    try {
      final response = await _dio.post(
        '${AppConstants.libreTranslateBaseUrl}/detect',
        data: {'q': text},
      );

      if (response.statusCode == 200) {
        final detections = response.data as List;
        if (detections.isNotEmpty) {
          return detections.first['language'] as String;
        }
        throw const ServerException(message: 'No language detected');
      }

      throw ServerException(
        message: 'Detection failed',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      throw ServerException(
        message: e.message ?? 'Network error during detection',
        statusCode: e.response?.statusCode,
      );
    }
  }
}
