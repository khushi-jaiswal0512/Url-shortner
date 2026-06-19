class ApiResponseModel<T> {
  final bool isSuccess;
  final T? data;
  final String? errorMessage;
  final int? statusCode;

  ApiResponseModel({
    required this.isSuccess,
    this.data,
    this.errorMessage,
    this.statusCode,
  });

  factory ApiResponseModel.success(T data, {int? statusCode}) {
    return ApiResponseModel(
      isSuccess: true,
      data: data,
      statusCode: statusCode,
    );
  }

  factory ApiResponseModel.error(String message, {int? statusCode}) {
    return ApiResponseModel(
      isSuccess: false,
      errorMessage: message,
      statusCode: statusCode,
    );
  }
}
