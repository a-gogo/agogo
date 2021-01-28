import { Location } from '@angular/common';
import { Component, OnInit, ViewChild } from '@angular/core';
import { NgModel } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import * as _ from 'lodash';
import * as moment from 'moment';
import { Subscription, timer } from 'rxjs';
import { ResourceService } from '../resource/resource.service';
import { DeploymentFilter } from '../deployment/deployment-filter';
import { DeploymentFilterType } from '../deployment/deployment-filter-type';
import { ComparatorFilterOption } from '../deployment/comparator-filter-option';
import { Deployment } from '../deployment/deployment';
import { DeploymentService } from '../deployment/deployment.service';
import { NavigationStoreService } from '../navigation/navigation-store.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DeploymentsEditModalComponent } from './deployments-edit-modal.component';
import { DateTimeModel } from '../shared/date-time-picker/date-time.model';
import { DeploymentsStore } from './deployment-container/deployments.store';

declare var $: any;

@Component({
  selector: 'amw-deployments',
  templateUrl: './deployments.component.html',
})
export class DeploymentsComponent implements OnInit {
  defaultComparator: string = 'eq';

  // initially by queryParam
  paramFilters: DeploymentFilter[] = [];
  autoload: boolean = true;

  // enhanced filters for deployment service
  filtersForBackend: DeploymentFilter[] = [];
  // value of filters parameter. Used to pass as json object to the logView.xhtml
  filtersForParam: DeploymentFilter[] = [];

  // valid for all, loaded once
  filterTypes: DeploymentFilterType[] = [];
  comparatorOptions: ComparatorFilterOption[] = [];
  comparatorOptionsMap: { [key: string]: string } = {};
  hasPermissionToRequestDeployments: boolean = false;
  csvSeparator: string = '';

  // available edit actions
  hasPermissionShakedownTest: boolean = false;
  deploymentDate: number; // for deployment date change

  // available filterValues (if any)
  filterValueOptions: { [key: string]: string[] } = {};

  // to be added
  selectedFilterType: DeploymentFilterType;

  // already set
  filters: DeploymentFilter[] = [];

  // filtered deployments
  deployments: Deployment[] = [];

  // csv export
  csvDocument: ArrayBuffer;

  // sorting with default values
  sortCol: string = 'd.deploymentDate';
  sortDirection: string = 'DESC';

  // pagination with default values
  maxResults: number = 10;
  offset: number = 0;
  allResults: number;
  currentPage: number;
  lastPage: number;

  // auto refresh
  refreshIntervals: number[] = [0, 5, 10, 30, 60, 120];
  refreshInterval: number = 0;
  timerSubscription: Subscription;

  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = true;

  @ViewChild('selectModel')
  selectModel: NgModel;

  constructor(
    private activatedRoute: ActivatedRoute,
    private location: Location,
    private deploymentService: DeploymentService,
    private resourceService: ResourceService,
    public navigationStore: NavigationStoreService,
    private modalService: NgbModal // public deploymentStore: DeploymentsStore
  ) {
    this.navigationStore.setVisible(false);
  }

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((param: any) => {
      if (param['filters']) {
        try {
          this.paramFilters = JSON.parse(param['filters']);
        } catch (e) {
          this.errorMessage = 'Error parsing filter';
          this.autoload = false;
        }
      } else {
        if (sessionStorage.getItem('deploymentFilters')) {
          this.paramFilters = JSON.parse(sessionStorage.getItem('deploymentFilters'));
        }
      }
      this.initTypeAndOptions();
      this.canRequestDeployments();
    });
  }

  addFilter() {
    if (this.selectedFilterType && this.canFilterBeAdded()) {
      const newFilter: DeploymentFilter = {} as DeploymentFilter;
      newFilter.name = this.selectedFilterType.name;
      newFilter.comp = this.defaultComparator;
      newFilter.val = this.selectedFilterType.type === 'booleanType' ? 'true' : '';
      newFilter.type = this.selectedFilterType.type;
      newFilter.compOptions = this.comparatorOptionsForType(this.selectedFilterType.type);
      this.setValueOptionsForFilter(newFilter);
      this.filters.push(newFilter);
      this.offset = 0;
      this.selectedFilterType = null;
      this.selectModel.reset(null);
    }
  }

  removeFilter(filter: DeploymentFilter) {
    const i: number = _.findIndex(this.filters, {
      name: filter.name,
      comp: filter.comp,
      val: filter.val,
    });
    if (i !== -1) {
      this.filters.splice(i, 1);
    }
    this.offset = 0;
  }

  clearFilters() {
    this.filters = [];
    sessionStorage.setItem('deploymentFilters', null);
    this.updateFiltersInURL(null);
  }

  applyFilters() {
    this.filtersForBackend = [];
    this.filtersForParam = [];
    const filtersToBeRemoved: DeploymentFilter[] = [];
    this.errorMessage = '';
    this.filters.forEach((filter) => {
      if (filter.val || filter.type === 'SpecialFilterType') {
        this.filtersForParam.push({
          name: filter.name,
          comp: filter.comp,
          val: filter.val,
        } as DeploymentFilter);
        if (filter.type === 'DateType') {
          if (!filter.val) {
            this.errorMessage = 'Invalid date';
          }
          this.filtersForBackend.push({
            name: filter.name,
            comp: filter.comp,
            val: filter.val.toEpoch().toString(),
          } as DeploymentFilter);
        } else {
          this.filtersForBackend.push({
            name: filter.name,
            comp: filter.comp,
            val: filter.val,
          } as DeploymentFilter);
        }
      } else {
        filtersToBeRemoved.push(filter);
      }
    });
    filtersToBeRemoved.forEach((filter) => this.removeFilter(filter));

    if (!this.errorMessage) {
      this.getFilteredDeployments(JSON.stringify(this.filtersForBackend));
      let filterString: string = null;
      if (this.filtersForParam.length > 0) {
        filterString = JSON.stringify(this.filtersForParam);
      }
      sessionStorage.setItem('deploymentFilters', filterString);
      this.updateFiltersInURL(filterString);
    }
  }

  changeDeploymentDate(deployment: Deployment) {
    if (deployment) {
      this.setDeploymentDate(deployment, deployment.deploymentDate);
    }
  }

  switchDeployments(enable: boolean) {
    this.deployments.forEach((deployment) => (deployment.selected = enable));
  }

  editableDeployments(): boolean {
    return _.findIndex(this.deployments, { selected: true }) !== -1;
  }

  showEdit() {
    if (!this.editableDeployments()) {
      return;
    }
    // get shakeDownTestPermission for first element
    const indexOfFirstSelectedElem = _.findIndex(this.deployments, {
      selected: true,
    });
    const firstDeployment = this.deployments[indexOfFirstSelectedElem];
    this.resourceService.canCreateShakedownTest(firstDeployment.appServerId).subscribe(
      /* happy path */ (r) => (this.hasPermissionShakedownTest = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        const modalRef = this.modalService.open(DeploymentsEditModalComponent);
        modalRef.componentInstance.deployments = this.getSelectedDeployments();
        modalRef.componentInstance.hasPermissionShakedownTest = this.hasPermissionShakedownTest;

        modalRef.componentInstance.doConfirmDeployment.subscribe((deployment: Deployment) =>
          this.confirmDeployment(deployment)
        );
        modalRef.componentInstance.doRejectDeployment.subscribe((deployment: Deployment) =>
          this.rejectDeployment(deployment)
        );
        modalRef.componentInstance.doCancelDeployment.subscribe((deployment: Deployment) =>
          this.cancelDeployment(deployment)
        );
        modalRef.componentInstance.doEditDeploymentDate.subscribe((deployment: Deployment) =>
          this.changeDeploymentDate(deployment)
        );
      }
    );
  }

  confirmDeployment(deployment: Deployment) {
    if (deployment) {
      delete deployment.selected;
      deployment.state = this.reMapState(deployment.state);
      this.deploymentService.confirmDeployment(deployment).subscribe(
        /* happy path */ (r) => r,
        /* error path */ (e) => (this.errorMessage = this.errorMessage ? this.errorMessage + '<br>' + e : e),
        /* onComplete */ () => this.reloadDeployment(deployment.id)
      );
    }
  }

  rejectDeployment(deployment: Deployment) {
    if (deployment) {
      this.deploymentService.rejectDeployment(deployment.id).subscribe(
        /* happy path */ (r) => r,
        /* error path */ (e) => (this.errorMessage = this.errorMessage ? this.errorMessage + '<br>' + e : e),
        /* onComplete */ () => this.reloadDeployment(deployment.id)
      );
    }
  }

  cancelDeployment(deployment: Deployment) {
    if (deployment) {
      this.deploymentService.cancelDeployment(deployment.id).subscribe(
        /* happy path */ (r) => r,
        /* error path */ (e) => (this.errorMessage = this.errorMessage ? this.errorMessage + '<br>' + e : e),
        /* onComplete */ () => this.reloadDeployment(deployment.id)
      );
    }
  }

  exportCSV() {
    this.isLoading = true;
    this.errorMessage = 'Generating your CSV.<br>Please hold on, depending on the requested data this may take a while';
    this.getFilteredDeploymentsForCsvExport(JSON.stringify(this.filtersForBackend));
  }

  copyURL() {
    const url: string = decodeURIComponent(window.location.href);
    $('body')
      .append($('<input type="text" name="fname" class="textToCopyInput" style="opacity:0"/>').val(url))
      .find('.textToCopyInput')
      .select();
    try {
      document.execCommand('copy');
    } catch (e) {
      window.prompt('Press Ctrl + C, then Enter to copy to clipboard', url);
    }
    $('.textToCopyInput').remove();
  }

  sortDeploymentsBy(col: string) {
    if (this.sortCol === col) {
      this.sortDirection = this.sortDirection === 'DESC' ? 'ASC' : 'DESC';
    } else {
      this.sortCol = col;
      this.sortDirection = 'DESC';
    }
    this.applyFilters();
  }

  setMaxResultsPerPage(max: number) {
    this.maxResults = max;
    this.offset = 0;
    this.applyFilters();
  }

  setNewOffset(offset: number) {
    this.offset = offset;
    this.applyFilters();
  }

  reloadDeployment(deploymentId: number) {
    let reloadedDeployment: Deployment;
    this.deploymentService.getWithActions(deploymentId).subscribe(
      /* happy path */ (r) => (reloadedDeployment = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* on complete */ () => this.updateDeploymentsList(reloadedDeployment)
    );
  }

  getSelectedDeployments(): Deployment[] {
    return this.deployments.filter((deployment) => deployment.selected === true);
  }

  autoRefresh() {
    if (this.refreshInterval > 0 && !this.timerSubscription) {
      this.timerSubscription = timer(this.refreshInterval * 1000).subscribe(() => {
        this.getFilteredDeployments(JSON.stringify(this.filtersForBackend));
        this.timerSubscription = null;
      });
    }
  }

  private canFilterBeAdded(): boolean {
    return (
      this.selectedFilterType.name !== 'Latest deployment job for App Server and Env' ||
      _.findIndex(this.filters, { name: this.selectedFilterType.name }) === -1
    );
  }

  private pushDownload(prefix: string) {
    this.isLoading = false;
    const docName: string = prefix + '_' + moment().format('YYYY-MM-DD_HHmm').toString() + '.csv';
    const blob = new Blob([this.csvDocument], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    if (navigator.msSaveOrOpenBlob) {
      navigator.msSaveBlob(blob, docName);
    } else {
      const a = document.createElement('a');
      a.href = url;
      a.download = docName;
      a.setAttribute('style', 'display:none;');
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    }
    window.URL.revokeObjectURL(url);
    this.errorMessage = null;
  }

  private setDeploymentDate(deployment: Deployment, deploymentDate: number) {
    this.deploymentService.setDeploymentDate(deployment.id, deploymentDate).subscribe(
      /* happy path */ (r) => r,
      /* error path */ (e) => (this.errorMessage = this.errorMessage ? this.errorMessage + '<br>' + e : e),
      /* on complete */ () => this.reloadDeployment(deployment.id)
    );
  }

  private updateDeploymentsList(deployment: Deployment) {
    this.deployments.splice(_.findIndex(this.deployments, { id: deployment.id }), 1, deployment);
  }

  private comparatorOptionsForType(filterType: string) {
    if (filterType === 'booleanType' || filterType === 'StringType' || filterType === 'ENUM_TYPE') {
      return [{ name: 'eq', displayName: 'is' }];
    } else {
      return this.comparatorOptions;
    }
  }

  private setValueOptionsForFilter(filter: DeploymentFilter) {
    if (!this.filterValueOptions[filter.name]) {
      if (filter.type === 'booleanType') {
        filter.valOptions = this.filterValueOptions[filter.name] = ['true', 'false'];
      } else {
        this.getAndSetFilterOptionValues(filter);
      }
    }
    filter.valOptions = this.filterValueOptions[filter.name];
  }

  private mapStates() {
    if (this.deployments) {
      this.deployments.forEach((deployment) => {
        switch (deployment.state) {
          case 'PRE_DEPLOYMENT':
            deployment.state = 'pre_deploy';
            break;
          case 'READY_FOR_DEPLOYMENT':
            deployment.state = 'ready_for_deploy';
            break;
          default:
            break;
        }
      });
    }
  }

  private reMapState(state: string) {
    switch (state) {
      case 'pre_deploy':
        return 'PRE_DEPLOYMENT';
      case 'ready_for_deploy':
        return 'READY_FOR_DEPLOYMENT';
      default:
        return state;
    }
  }

  private initTypeAndOptions() {
    this.isLoading = true;
    this.deploymentService.getAllDeploymentFilterTypes().subscribe(
      /* happy path */ (r) => (this.filterTypes = _.sortBy(r, 'name')),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => this.getAllComparatorOptions()
    );
  }

  private getAllComparatorOptions() {
    this.deploymentService.getAllComparatorFilterOptions().subscribe(
      /* happy path */ (r) => (this.comparatorOptions = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        this.populateMap();
        this.enhanceParamFilter();
      }
    );
  }

  private getAndSetFilterOptionValues(filter: DeploymentFilter) {
    this.deploymentService.getFilterOptionValues(filter.name).subscribe(
      /* happy path */ (r) => (this.filterValueOptions[filter.name] = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (filter.valOptions = this.filterValueOptions[filter.name])
    );
  }

  private getFilteredDeployments(filterString: string) {
    this.isLoading = true;
    this.deploymentService
      .getFilteredDeployments(filterString, this.sortCol, this.sortDirection, this.offset, this.maxResults)
      .subscribe(
        /* happy path */ (r) => {
          this.deployments = r.deployments;
          this.allResults = r.total;
          this.currentPage = Math.floor(this.offset / this.maxResults) + 1;
          this.lastPage = Math.ceil(this.allResults / this.maxResults);
        },
        /* error path */ (e) => {
          this.errorMessage = e;
          this.isLoading = false;
        },
        /* onComplete */ () => {
          this.isLoading = false;
          this.mapStates();
          this.autoRefresh();
        }
      );
  }

  private getFilteredDeploymentsForCsvExport(filterString: string) {
    this.deploymentService.getFilteredDeploymentsForCsvExport(filterString, this.sortCol, this.sortDirection).subscribe(
      /* happy path */ (r) => (this.csvDocument = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => this.pushDownload('deployments')
    );
  }

  private canRequestDeployments() {
    this.deploymentService.canRequestDeployments().subscribe(
      /* happy path */ (r) => (this.hasPermissionToRequestDeployments = r),
      /* error path */ (e) => (this.errorMessage = e)
    );
  }

  private enhanceParamFilter() {
    if (this.paramFilters) {
      this.clearFilters();
      this.paramFilters.forEach((filter) => {
        const i: number = _.findIndex(this.filterTypes, ['name', filter.name]);
        if (i >= 0) {
          filter.type = this.filterTypes[i].type;
          filter.compOptions = this.comparatorOptionsForType(filter.type);
          filter.comp = !filter.comp ? this.defaultComparator : filter.comp;
          this.parseDateTime(filter);
          this.setValueOptionsForFilter(filter);
          this.filters.push(filter);
        } else {
          this.errorMessage = 'Error parsing filter';
        }
      });
    }
    if (this.autoload) {
      this.applyFilters();
    }
  }

  // parse string from json back to DateTimeModel
  private parseDateTime(filter: DeploymentFilter) {
    if (filter.type === 'DateType') {
      filter.val = DateTimeModel.fromLocalString(filter.val);
    }
  }

  private populateMap() {
    this.comparatorOptions.forEach((option) => {
      this.comparatorOptionsMap[option.name] = option.displayName;
    });
    this.isLoading = false;
  }

  private updateFiltersInURL(destination: string) {
    if (destination) {
      this.location.replaceState('/deployments?filters=' + destination);
    } else {
      this.location.replaceState('/deployments');
    }
  }
}
