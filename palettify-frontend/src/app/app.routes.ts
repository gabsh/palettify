import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home';
import { LibraryComponent } from './components/library/library';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'library', component: LibraryComponent }
];