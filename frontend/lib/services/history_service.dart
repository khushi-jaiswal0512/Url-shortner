import '../models/api_response_model.dart';
import '../models/url_model.dart';
import 'api_constants.dart';
import 'api_service.dart';

class HistoryService {
  final ApiService _apiService;

  HistoryService(this._apiService);

  Future<ApiResponseModel<List<UrlModel>>> getAllUrls() {
    return _apiService.get<List<UrlModel>>(
      ApiConstants.getUrls,
      fromJsonList: (list) => list.map((e) => UrlModel.fromJson(e)).toList(),
    );
  }
}
