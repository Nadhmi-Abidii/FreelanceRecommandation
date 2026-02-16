import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
// CoreUI Angular v5 modules
import {
  NavbarModule, DropdownModule, CollapseModule, GridModule,
  ButtonModule, FormModule, NavModule
} from '@coreui/angular';
import { AuthService } from '../auth/auth';

@Component({
  standalone: true,
  selector: 'app-main-navbar',
  imports: [
    CommonModule,
    // CoreUI
    NavbarModule, DropdownModule, CollapseModule, GridModule,
    ButtonModule, FormModule, NavModule
  ],
  template: `
  <c-navbar colorScheme="light" expand="lg" [ngStyle]="{ backgroundColor: '#e3f2fd' }">
    <c-container fluid>
      <a cNavbarBrand routerLink="/dashboard">FreelancePro</a>

      <button [cNavbarToggler]="collapseRef"></button>

      <div #collapseRef="cCollapse" navbar cCollapse>
        <c-navbar-nav class="me-auto mb-2 mb-lg-0">
          <c-nav-item>
            <a cNavLink routerLink="/dashboard" routerLinkActive="active" [active]>Tableau de bord</a>
          </c-nav-item>
          <c-nav-item>
            <a cNavLink routerLink="/missions/new" routerLinkActive="active">Creer une mission</a>
          </c-nav-item>
          <c-nav-item>
            <a cNavLink routerLink="/missions" routerLinkActive="active">Mes missions</a>
          </c-nav-item>

          <c-dropdown variant="nav-item" [popper]="false">
            <a cDropdownToggle cNavLink>Candidatures</a>
            <ul cDropdownMenu [ngStyle]="{ backgroundColor: '#e3f2fd' }">
              <li><button cDropdownItem routerLink="/candidatures">Toutes</button></li>
              <li><button cDropdownItem routerLink="/candidatures?filter=en-cours">En cours</button></li>
              <li><button cDropdownItem routerLink="/candidatures?filter=archivees">Archivees</button></li>
            </ul>
          </c-dropdown>

          <c-nav-item>
            <a cNavLink routerLink="/discussions" routerLinkActive="active">Discussions</a>
          </c-nav-item>
          <c-nav-item *ngIf="isClient()">
            <a cNavLink routerLink="/client/wallet" routerLinkActive="active">Wallet</a>
          </c-nav-item>

          <c-nav-item *ngIf="isFreelancer()">
            <a cNavLink routerLink="/freelancer/portfolio" routerLinkActive="active">Portfolio</a>
          </c-nav-item>

          <c-nav-item *ngIf="isFreelancer()">
            <a cNavLink routerLink="/freelancer/candidatures" routerLinkActive="active">Mes candidatures</a>
          </c-nav-item>

          <c-nav-item *ngIf="isLoggedIn()">
            <a cNavLink routerLink="/messages" routerLinkActive="active">Messages</a>
          </c-nav-item>
        </c-navbar-nav>

        <form cForm class="d-flex" role="search">
          <input cFormControl type="search" placeholder="Rechercher" aria-label="Search" class="me-2">
          <button cButton color="success" variant="outline">Search</button>
        </form>
      </div>
    </c-container>
  </c-navbar>
  `,
  styles: [`
    :host ::ng-deep .active { font-weight: 600; }
  `]
})
export default class MainNavbarComponent {
  private readonly auth = inject(AuthService);

  readonly isClient = computed(() => (this.auth.role() || '').toUpperCase() === 'ROLE_CLIENT');
  readonly isFreelancer = computed(
    () => (this.auth.role() || '').toUpperCase() === 'ROLE_FREELANCER'
  );
  readonly isLoggedIn = computed(() => !!this.auth.isLoggedIn());
}
