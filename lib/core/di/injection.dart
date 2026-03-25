import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import 'package:get_it/get_it.dart';
import 'package:translator_keyboard/core/network/network_info.dart';
import 'package:translator_keyboard/core/utils/debouncer.dart';
import 'package:translator_keyboard/features/translation/data/datasources/translation_remote_datasource.dart';
import 'package:translator_keyboard/features/translation/data/datasources/translation_remote_datasource_impl.dart';
import 'package:translator_keyboard/features/translation/data/repositories/translation_repository_impl.dart';
import 'package:translator_keyboard/features/translation/domain/repositories/translation_repository.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/detect_language.dart';
import 'package:translator_keyboard/features/translation/domain/usecases/translate_text.dart';
import 'package:translator_keyboard/features/translation/presentation/bloc/translation_bloc.dart';

final getIt = GetIt.instance;

void configureDependencies() {
  // External
  getIt.registerLazySingleton<Dio>(() => Dio());
  getIt.registerLazySingleton<Connectivity>(() => Connectivity());

  // Core
  getIt.registerLazySingleton<NetworkInfo>(
    () => NetworkInfoImpl(getIt<Connectivity>()),
  );

  // Data sources
  getIt.registerLazySingleton<TranslationRemoteDataSource>(
    () => TranslationRemoteDataSourceImpl(getIt<Dio>()),
  );

  // Repositories
  getIt.registerLazySingleton<TranslationRepository>(
    () => TranslationRepositoryImpl(
      remoteDataSource: getIt<TranslationRemoteDataSource>(),
      networkInfo: getIt<NetworkInfo>(),
    ),
  );

  // Use cases
  getIt.registerLazySingleton(
    () => TranslateText(getIt<TranslationRepository>()),
  );
  getIt.registerLazySingleton(
    () => DetectLanguage(getIt<TranslationRepository>()),
  );

  // BLoC
  getIt.registerFactory(
    () => TranslationBloc(
      translateText: getIt<TranslateText>(),
      detectLanguage: getIt<DetectLanguage>(),
      debouncer: Debouncer(),
    ),
  );
}
