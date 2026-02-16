import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { of } from 'rxjs';

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    tap({
      error: (err: HttpErrorResponse) => {
        if (err.status === 401 || err.status === 403) {
          // Optionnel: nettoyer token
          // localStorage.removeItem('token');
          // localStorage.removeItem('role');
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
      }
    })
  );
};
