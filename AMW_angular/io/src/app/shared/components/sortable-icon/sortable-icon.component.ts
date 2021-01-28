import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-sortable-icon',
  template: ` <app-icon icon="caret-{{ direction }}-fill"></app-icon> `,
  styles: [],
})
export class SortableIconComponent {
  @Input() sortDirection: SortDirection;

  direction: string;

  constructor() {
    this.direction = this.sortDirection === 'ASC' ? 'up' : 'down';
  }
}

type SortDirection = 'ASC' | 'DESC';
