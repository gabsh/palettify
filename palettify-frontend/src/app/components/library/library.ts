import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { NgIf, NgFor, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink, Router } from '@angular/router';
import { TimeAgoPipe } from '../../pipes/time-ago-pipe';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';

interface ColorInfo {
  hex: string;
  r: number;
  g: number;
  b: number;
  frequency: number;
}

interface LibraryItem {
  id: number;
  domain: string;
  url: string;
  colors: ColorInfo[];
  fonts: string[];
  faviconUrl: string;
  createdAt: string;
  updatedAt: string;
}

interface PageResponse {
  content: LibraryItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  last: boolean;
}

@Component({
  selector: 'app-library',
  imports: [NgIf, NgFor, RouterLink, TimeAgoPipe, FormsModule],
  templateUrl: './library.html',
  styleUrl: './library.scss',
})
export class LibraryComponent implements OnInit {
  items: LibraryItem[] = [];
  loading: boolean = false;
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  isLast: boolean = false;
  searchQuery: string = '';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadLibrary();
  }

  loadLibrary(page: number = 0) {
    this.loading = true;
    this.http.get<PageResponse>(
      `${environment.apiUrl}/api/palette/library?page=${page}&size=9`
    ).subscribe({
      next: (res) => {
        this.items = page === 0 ? res.content : [...this.items, ...res.content];
        this.currentPage = res.number;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.isLast = res.last;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadMore() {
    this.loadLibrary(this.currentPage + 1);
  }

  goToSite(domain: string) {
    this.router.navigate(['/'], { queryParams: { url: 'https://' + domain } });
  }

  get filteredItems(): LibraryItem[] {
    if (!this.searchQuery) return this.items;
    return this.items.filter(item =>
      item.domain.toLowerCase().includes(this.searchQuery.toLowerCase())
    );
  }
}