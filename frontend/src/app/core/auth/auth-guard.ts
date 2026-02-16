import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = (route, state) => {
  const token = localStorage.getItem('token');
  if (token) return true;

  const router = new Router();
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
