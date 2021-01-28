import { NgModule } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AuditviewComponent } from './auditview.component';
import { AuditviewRoutingModule } from './auditview-routing.module';
import { AuditviewService } from './auditview.service';
import { AuditviewTableComponent } from './auditview-table/auditview-table.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { BaseModule } from '../base/base.module';
import { SharedModule } from '../shared/shared.module';
import { NewlineFilterPipe } from './auditview-table/newlineFilterPipe';
import { SortableHeader } from './auditview-table/sortable.directive';

@NgModule({
  imports: [CommonModule, SharedModule, AuditviewRoutingModule, NgbModule, BaseModule],
  declarations: [AuditviewComponent, NewlineFilterPipe, AuditviewTableComponent, SortableHeader],
  providers: [AuditviewService, DatePipe],
})
export class AuditviewModule {}
