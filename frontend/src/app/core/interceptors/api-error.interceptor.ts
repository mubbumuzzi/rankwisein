import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const message =
        err.error?.message ??
        (err.status === 0
          ? 'Cannot reach the server. Is the backend running?'
          : `Request failed (${err.status})`);
      return throwError(() => ({ status: err.status, message, raw: err }));
    })
  );
