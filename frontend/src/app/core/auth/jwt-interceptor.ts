import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../src/environments/environment';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method === 'OPTIONS') return next(req);
  if (!req.url.startsWith(environment.apiUrl)) return next(req);

  const token = localStorage.getItem('token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
