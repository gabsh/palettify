import { Component, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf, NgFor, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { environment } from '../../../environments/environment';

interface ColorInfo {
  hex: string;
  r: number;
  g: number;
  b: number;
  frequency: number;
}

interface PaletteResponse {
  url: string;
  domain: string;
  colors: ColorInfo[];
  fonts: string[];
  faviconUrl: string;
}

@Component({
  selector: 'app-home',
  imports: [FormsModule, NgIf, NgFor, DecimalPipe, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomeComponent implements OnInit, OnDestroy {
  url = '';
  response: PaletteResponse | null = null;
  loading = false;
  error = '';
  copiedColor = '';
  spinnerChar = '';
  showPopup = true;

  private readonly spinnerChars = '⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏';
  private spinnerIdx = 0;
  private spinnerInterval: ReturnType<typeof setInterval> | null = null;
  private toastTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const urlParam = this.route.snapshot.queryParamMap.get('url');
    if (urlParam) {
      this.url = urlParam;
      this.extract();
    }
  }

  ngOnDestroy() {
    if (this.spinnerInterval) clearInterval(this.spinnerInterval);
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
  }

  private startSpinner() {
    this.spinnerIdx = 0;
    this.spinnerChar = this.spinnerChars[0];
    this.spinnerInterval = setInterval(() => {
      this.spinnerIdx = (this.spinnerIdx + 1) % this.spinnerChars.length;
      this.spinnerChar = this.spinnerChars[this.spinnerIdx];
      this.cdr.detectChanges();
    }, 80);
  }

  private stopSpinner() {
    if (this.spinnerInterval) {
      clearInterval(this.spinnerInterval);
      this.spinnerInterval = null;
    }
    this.spinnerChar = '';
  }

  extract(force = false) {
    if (!this.url) return;

    if (!this.url.startsWith('http://') && !this.url.startsWith('https://')) {
      this.url = 'https://' + this.url;
    }

    this.loading = true;
    this.error = '';
    this.response = null;
    this.startSpinner();
    this.cdr.detectChanges();

    this.http.post<PaletteResponse>(
      `${environment.apiUrl}/api/palette?force=${force}`,
      { url: this.url }
    ).subscribe({
      next: (res) => {
        this.response = res;
        this.loading = false;
        this.stopSpinner();
        this.cdr.detectChanges();
      },
      error: (err) => {
        switch (err.status) {
          case 400: this.error = 'invalid url or private address.'; break;
          case 403: this.error = 'this website blocks automated access.'; break;
          case 429: this.error = 'rate limited — please wait before retrying.'; break;
          case 504: this.error = 'website took too long to respond.'; break;
          default:  this.error = 'failed to analyze this website.';
        }
        this.loading = false;
        this.stopSpinner();
        this.cdr.detectChanges();
      }
    });
  }

  closePopup() {
    this.showPopup = false;
  }

  copyToClipboard(hex: string) {
    navigator.clipboard.writeText(hex);
    this.copiedColor = hex;
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.copiedColor = '';
      this.cdr.detectChanges();
    }, 1500);
    this.cdr.detectChanges();
  }
}
