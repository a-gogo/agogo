import { Component, Input, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'amw-pagination',
  templateUrl: './pagination.component.html'
})
export class PaginationComponent {
  @Input() currentPage: number;
  @Input() lastPage: number;
  @Output() doSetOffset: EventEmitter<number> = new EventEmitter<number>();
  @Output() doSetMax: EventEmitter<number> = new EventEmitter<number>();

  paginatorItems: number = 5;

  maxResults: number = 10;

  constructor() {}

  pages(): number[] {
    if (this.lastPage > 1) {
      const itemsBefore: number = Math.floor(this.paginatorItems / 2);
      const start: number =
        this.currentPage > itemsBefore ? this.currentPage - itemsBefore : 1;
      const end: number = start + this.paginatorItems - 1;
      return this.range(start, end < this.lastPage ? end : this.lastPage);
    }
    return;
  }

  toPage(page: number) {
    if (page <= this.lastPage && page !== this.currentPage) {
      page = page > 0 ? page - 1 : 0;
      this.doSetOffset.emit(page * this.maxResults);
    }
  }

  setMax() {
    this.doSetMax.emit(this.maxResults);
  }

  private range(a: number, b: number): number[] {
    const d: number[] = [];
    let c: number = b - a + 1;
    while (c--) {
      d[c] = b--;
    }
    return d;
  }
}
