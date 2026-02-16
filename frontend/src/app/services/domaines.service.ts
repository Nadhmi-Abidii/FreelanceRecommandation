import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../src/environments/environment';
import { ApiResponse } from '../core/api/api-response.model';
import { Domaine, DomainePage } from '../models/domaine.model';
import { AiDomainSuggestion, AiDomainSuggestionRequest } from './ai.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DomainesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/domaines`;

  create(payload: Partial<Domaine>): Observable<Domaine> {
    return this.http.post<ApiResponse<Domaine>>(this.base, payload).pipe(map(r => r.data));
  }

  update(id: number, payload: Partial<Domaine>): Observable<Domaine> {
    return this.http.put<ApiResponse<Domaine>>(`${this.base}/${id}`, payload).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse>(`${this.base}/${id}`).pipe(map(() => void 0));
  }

  getById(id: number): Observable<Domaine> {
    return this.http.get<ApiResponse<Domaine>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  listPaged(page = 0, size = 10): Observable<DomainePage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<DomainePage>>(this.base, { params }).pipe(map(r => r.data));
  }

  listActive(): Observable<Domaine[]> {
    return this.http.get<ApiResponse<Domaine[]>>(`${this.base}/active`).pipe(map(r => r.data));
  }

  search(keyword: string): Observable<Domaine[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<ApiResponse<Domaine[]>>(`${this.base}/search`, { params }).pipe(map(r => r.data));
  }

  activate(id: number): Observable<Domaine> {
    return this.http.put<ApiResponse<Domaine>>(`${this.base}/${id}/activate`, {}).pipe(map(r => r.data));
  }

  deactivate(id: number): Observable<Domaine> {
    return this.http.put<ApiResponse<Domaine>>(`${this.base}/${id}/deactivate`, {}).pipe(map(r => r.data));
  }

  suggest(payload: AiDomainSuggestionRequest): Observable<AiDomainSuggestion[]> {
    return this.http
      .post<ApiResponse<AiDomainSuggestion[]>>(`${this.base}/suggest`, payload)
      .pipe(map(r => r.data ?? []));
  }
}
