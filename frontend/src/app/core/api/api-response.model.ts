export interface ApiResponse<T = unknown> {
  message: string;
  success: boolean;
  data: T;
}
