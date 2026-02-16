import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { appConfig } from './app/app.config';

@Component({
  standalone: true,
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
class AppComponent {}

bootstrapApplication(AppComponent, appConfig)
  .catch(err => console.error(err));
