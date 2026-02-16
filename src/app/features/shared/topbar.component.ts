import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../services/auth.service';

@Component({
  standalone: true,
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss'],
  imports: [
    CommonModule,
    RouterLink, RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatSlideToggleModule,
    MatDividerModule
  ]
})
export default class TopbarComponent {
  private readonly auth = inject(AuthService);

  readonly isClient = computed(() => (this.auth.role() || '').toUpperCase() === 'ROLE_CLIENT');
  readonly isFreelancer = computed(() => (this.auth.role() || '').toUpperCase() === 'ROLE_FREELANCER');
  readonly isAdmin = computed(() => (this.auth.role() || '').toUpperCase() === 'ROLE_ADMIN');

  dark = signal(false);

  toggleDark(enabled: boolean) {
    this.dark.set(enabled);
    document.documentElement.classList.toggle('dash-dark', enabled);
  }

  logout() {
    this.auth.logout();
  }
}
