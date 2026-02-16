// src/app/features/admin/domaines/competences.service.ts

import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../src/environments/environment';
import { ApiResponse } from '../core/api/api-response.model';
import { Competence, CompetencePage } from '../models/competence.model';

@Injectable({ providedIn: 'root' })
export class CompetencesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/competences`;

  // Liste paginée
  listPaged(page = 0, size = 10): Observable<CompetencePage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http
      .get<ApiResponse<CompetencePage>>(this.base, { params })
      .pipe(map(r => r.data));
  }

  // Création
  create(payload: Partial<Competence>): Observable<Competence> {
    return this.http
      .post<ApiResponse<Competence>>(this.base, payload)
      .pipe(map(r => r.data));
  }

  // Mise à jour
  update(id: number, payload: Partial<Competence>): Observable<Competence> {
    return this.http
      .put<ApiResponse<Competence>>(`${this.base}/${id}`, payload)
      .pipe(map(r => r.data));
  }

  // Suppression
  delete(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse>(`${this.base}/${id}`)
      .pipe(map(() => void 0));
  }

  // Certification éventuelle
  certify(id: number, certificationName: string): Observable<Competence> {
    const params = new HttpParams().set('certificationName', certificationName);
    return this.http
      .put<ApiResponse<Competence>>(`${this.base}/${id}/certify`, null, { params })
      .pipe(map(r => r.data));
  }

  // ✅ Activer une compétence
  activate(id: number): Observable<Competence> {
    return this.http
      .put<ApiResponse<Competence>>(`${this.base}/${id}/activate`, {})
      .pipe(map(r => r.data));
  }

  // ✅ Désactiver une compétence
  deactivate(id: number): Observable<Competence> {
    return this.http
      .put<ApiResponse<Competence>>(`${this.base}/${id}/deactivate`, {})
      .pipe(map(r => r.data));
  }
}
