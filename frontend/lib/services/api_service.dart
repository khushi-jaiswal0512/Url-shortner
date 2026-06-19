import 'package:dio/dio.dart';
import '../models/api_response_model.dart';

class ApiService {
  final Dio _dio;

  ApiService(String baseUrl) : _dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
    headers: {
      'Content-Type': 'application/json',
    },
  ));

  void updateBaseUrl(String baseUrl) {
    _dio.options.baseUrl = baseUrl;
  }

  Future<ApiResponseModel<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    T Function(Map<String, dynamic>)? fromJson,
    T Function(List<dynamic>)? fromJsonList,
  }) async {
    try {
      final response = await _dio.get(path, queryParameters: queryParameters);
      if (response.statusCode == 200 || response.statusCode == 201) {
        if (fromJson != null) {
          return ApiResponseModel.success(fromJson(response.data), statusCode: response.statusCode);
        } else if (fromJsonList != null) {
          return ApiResponseModel.success(fromJsonList(response.data), statusCode: response.statusCode);
        }
        return ApiResponseModel.success(response.data, statusCode: response.statusCode);
      }
      return ApiResponseModel.error('Unexpected status code: ${response.statusCode}', statusCode: response.statusCode);
    } on DioException catch (e) {
      return ApiResponseModel.error(_handleDioError(e), statusCode: e.response?.statusCode);
    } catch (e) {
      return ApiResponseModel.error('Unexpected error: $e');
    }
  }

  Future<ApiResponseModel<T>> post<T>(
    String path, {
    dynamic data,
    T Function(Map<String, dynamic>)? fromJson,
  }) async {
    try {
      final response = await _dio.post(path, data: data);
      if (response.statusCode == 200 || response.statusCode == 201) {
        if (fromJson != null) {
          return ApiResponseModel.success(fromJson(response.data), statusCode: response.statusCode);
        }
        return ApiResponseModel.success(response.data, statusCode: response.statusCode);
      }
      return ApiResponseModel.error('Unexpected status code: ${response.statusCode}', statusCode: response.statusCode);
    } on DioException catch (e) {
      return ApiResponseModel.error(_handleDioError(e), statusCode: e.response?.statusCode);
    } catch (e) {
      return ApiResponseModel.error('Unexpected error: $e');
    }
  }

  Future<ApiResponseModel<void>> delete(String path) async {
    try {
      final response = await _dio.delete(path);
      if (response.statusCode == 204 || response.statusCode == 200) {
        return ApiResponseModel.success(null, statusCode: response.statusCode);
      }
      return ApiResponseModel.error('Unexpected status code: ${response.statusCode}', statusCode: response.statusCode);
    } on DioException catch (e) {
      return ApiResponseModel.error(_handleDioError(e), statusCode: e.response?.statusCode);
    } catch (e) {
      return ApiResponseModel.error('Unexpected error: $e');
    }
  }

  String _handleDioError(DioException e) {
    if (e.response != null && e.response?.data != null) {
      try {
        final data = e.response?.data;
        if (data is Map && data.containsKey('message')) {
          return data['message'];
        }
      } catch (_) {}
    }
    
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Connection timeout. Please try again.';
      case DioExceptionType.badResponse:
        return 'Server returned an error: ${e.response?.statusCode}';
      case DioExceptionType.connectionError:
        return 'Connection error. Please check your internet or server URL.';
      default:
        return 'Network error occurred.';
    }
  }
}
