import '../models/api_response_model.dart';
import '../models/dashboard_model.dart';
import 'api_constants.dart';
import 'api_service.dart';

class DashboardService {
  final ApiService _apiService;

  DashboardService(this._apiService);

  Future<ApiResponseModel<DashboardModel>> getDashboardStats() {
    return _apiService.get<DashboardModel>(
      ApiConstants.getDashboard,
      fromJson: (json) => DashboardModel.fromJson(json),
    );
  }
}
