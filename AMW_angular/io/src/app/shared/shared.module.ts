import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import * as sharedComponents from './components';

@NgModule({
  declarations: [...sharedComponents.components],
  imports: [CommonModule, FormsModule, NgbModule],
  exports: [FormsModule, NgbModule, ReactiveFormsModule, ...sharedComponents.components],
})
export class SharedModule {}
