import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuditviewService } from './auditview.service';
import { ResourceService } from '../resource/resource.service';
import { AuditLogEntry } from './auditview-entry';
import { NavigationStoreService } from '@core/services';

@Component({
  selector: 'amw-auditview',
  templateUrl: './auditview.component.html',
})
export class AuditviewComponent implements OnInit {
  name: string;
  auditLogEntries: AuditLogEntry[] = [];
  filterQuery: string = '';
  errorMessage: string;
  successMessage: string;
  resourceId: number;
  isLoading: boolean = true;

  constructor(
    public navigationStore: NavigationStoreService,
    private auditViewService: AuditviewService,
    private resourceService: ResourceService,
    private activatedRoute: ActivatedRoute
  ) {
    this.navigationStore.setVisible(false);
    this.navigationStore.setPageTitle('Audit View');
  }

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((param: any) => {
      if (param['resourceId']) {
        try {
          this.resourceId = JSON.parse(param['resourceId']);
        } catch (e) {
          console.error(e);
          this.errorMessage = 'Error parsing resourceId';
        }
      }
    });

    if (this.resourceId) {
      this.resourceService.getResourceName(this.resourceId).subscribe(
        /* happy path */ (r) => (this.name = r.name),
        /* error path */ (e) => (this.errorMessage = e),
        /* onComplete */ () => {}
      );
      this.auditViewService.getAuditLogForResource(this.resourceId).subscribe(
        /* happy path */ (r) => (this.auditLogEntries = r),
        /* error path */ (e) => (this.errorMessage = e),
        /* onComplete */ () => (this.isLoading = false)
      );
    } else {
      console.error('Resource Id must be set');
    }
  }
}
