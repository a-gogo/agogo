import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-indicator',
  template: `
    <div
      *ngIf="isLoading"
      class="d-flex justify-content-center align-items-center overlay"
    >
      <div class="spinner-border text-light" role="status">
        <span class="sr-only">Loading...</span>
      </div>
    </div>
    <div class=""></div>
  `,
  styles: [
    `
      .overlay {
        opacity: 0.5;
        background: #000;
        width: 100%;
        height: 100%;
        z-index: 9999;
        top: 0;
        left: 0;
        position: fixed !important;
      }
    `,
  ],
})
export class LoadingIndicatorComponent {
  @Input()
  isLoading: boolean;

  constructor() {}
}
