import '../models/api_response_model.dart';
import '../models/url_model.dart';
import 'api_constants.dart';
import 'api_service.dart';

class UrlService {
  final ApiService _apiService;

  UrlService(this._apiService);

  Future<ApiResponseModel<UrlModel>> createShortUrl(String longUrl, {String? customAlias, DateTime? expiresAt}) {
    final Map<String, dynamic> data = {'longUrl': longUrl};
    if (customAlias != null && customAlias.isNotEmpty) {
      data['customAlias'] = customAlias;
    }
    if (expiresAt != null) {
      data['expiresAt'] = expiresAt.toIso8601String();
    }
    
    return _apiService.post<UrlModel>(
      ApiConstants.createUrl,
      data: data,
      fromJson: (json) => UrlModel.fromJson(json),
    );
  }

  Future<ApiResponseModel<void>> deleteUrl(String shortCode) {
    return _apiService.delete(ApiConstants.deleteUrl(shortCode));
  }
}
