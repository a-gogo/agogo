import { Component, OnInit, NgZone } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AppState } from '../app.service';
import { ComparatorFilterOption } from './comparator-filter-option';
import { DeploymentFilter } from './deployment-filter';
import { DeploymentFilterType } from './deployment-filter-type';
import { DeploymentService } from './deployment.service';
import { Datetimepicker } from 'eonasdan-bootstrap-datetimepicker';
import * as _ from 'lodash';

declare var $: any;

@Component({
  selector: 'amw-deployments',
  templateUrl: './deployments.component.html'
})

export class DeploymentsComponent implements OnInit {

  defaultComparator: string = 'eq';

  // initially by queryParam
  paramFilters: DeploymentFilter[] = [];

  // valid for all, loaded once
  filterTypes: DeploymentFilterType[] = [];
  comparatorOptions: ComparatorFilterOption[] = [];
  comparatorOptionsMap: { [key: string]: string } = {};

  // available filterValues (if any)
  filterValueOptions: { [key: string]: string[] } = {};

  // to be added
  selectedFilterType: DeploymentFilterType;

  // already set
  filters: DeploymentFilter[] = [];

  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;

  constructor(private activatedRoute: ActivatedRoute,
              private ngZone: NgZone,
              private location: Location,
              private deploymentService: DeploymentService,
              public appState: AppState) {
  }

  ngOnInit() {

    this.appState.set('navShow', false);
    this.appState.set('navTitle', 'Deployments');
    this.appState.set('pageTitle', 'Deployments');

    console.log('hello `Deployments` component');

    this.activatedRoute.queryParams.subscribe(
      (param: any) => {
        if (param['filter']) {
          try {
            this.paramFilters = JSON.parse(param['filter']);
          } catch (e) {
            console.log(e);
            this.errorMessage = 'Error parsing filter';
          }
        }
    });

    this.initTypeAndOptions();

  }

  addFilter() {
    if (this.selectedFilterType) {
      let newFilter: DeploymentFilter = <DeploymentFilter> {};
      newFilter.name = this.selectedFilterType.name;
      newFilter.comp = this.defaultComparator;
      newFilter.val = this.selectedFilterType.type === 'booleanType' ? 'true' : '';
      newFilter.type = this.selectedFilterType.type;
      newFilter.compOptions = this.comparatorOptionsForType(this.selectedFilterType.type);
      this.setValueOptionsForFilter(newFilter);
      this.filters.unshift(newFilter);
      this.enableDatepicker(newFilter.type);
    }
  }

  removeFilter(filter: DeploymentFilter) {
    _.remove(this.filters, {name: filter.name, comp: filter.comp, val: filter.val});
  }

  applyFilter() {
    let finalFilters: DeploymentFilter[] = [];
    console.log(this.filters);
    this.filters.forEach((filter) => {
      finalFilters.push(<DeploymentFilter> {name: filter.name, comp: filter.comp, val: filter.val});
    });
    console.log(finalFilters);
    this.goTo(JSON.stringify(finalFilters));
  }

  private enableDatepicker(filterType: string) {
    if (filterType === 'DateType') {
      this.ngZone.onMicrotaskEmpty.first().subscribe(() => {
        $('.datepicker').datetimepicker({format: 'DD.MM.YYYY HH:mm'});
      });
    }
  }

  private comparatorOptionsForType(filterType: string) {
    if (filterType === 'booleanType' || filterType === 'StringType' || filterType === 'ENUM_TYPE') {
      return [{name: 'eq', displayName: 'is'}];
    } else {
      return this.comparatorOptions;
    }
  }

  private setValueOptionsForFilter(filter: DeploymentFilter) {
    console.log('valueOptionsForFilter ' + filter.name + ', ' + filter.type);
    if (!this.filterValueOptions[filter.name]) {
      if (filter.type === 'booleanType') {
        filter.valOptions = this.filterValueOptions[filter.name] = [ 'true', 'false' ];
      } else {
        this.getAndSetFilterOptionValues(filter);
      }
    }
    filter.valOptions = this.filterValueOptions[filter.name];
  }

  private initTypeAndOptions() {
    this.isLoading = true;
    this.deploymentService.getAllDeploymentFilterTypes().subscribe(
      /* happy path */ (r) => this.filterTypes = _.sortBy(r, 'name'),
      /* error path */ (e) => this.errorMessage = e,
      /* onComplete */ () => this.getAllComparatorOptions());
  }

  private getAllComparatorOptions() {
    this.deploymentService.getAllComparatorFilterOptions().subscribe(
      /* happy path */ (r) => this.comparatorOptions = r,
      /* error path */ (e) => this.errorMessage = e,
      /* onComplete */ () => { this.populateMap();
                               this.enhanceParamFilter();
      });
  }

  private getAndSetFilterOptionValues(filter: DeploymentFilter) {
    this.deploymentService.getFilterOptionValues(filter.name).subscribe(
      /* happy path */ (r) => this.filterValueOptions[filter.name] = r,
      /* error path */ (e) => this.errorMessage = e,
      /* onComplete */ () => filter.valOptions = this.filterValueOptions[filter.name]);
  }

  private enhanceParamFilter() {
    if (this.paramFilters) {
      this.paramFilters.forEach((filter) => {
        let i: number = _.findIndex(this.filterTypes, ['name', filter.name]);
        if (i >= 0) {
          filter.type = this.filterTypes[i].type;
          filter.compOptions = this.comparatorOptionsForType(filter.type);
          filter.comp = !filter.comp ? this.defaultComparator : filter.comp;
          this.setValueOptionsForFilter(filter);
          this.filters.push(filter);
          this.enableDatepicker(filter.type);
        } else {
          this.errorMessage = 'Error parsing filter';
        }
      });
    }
    this.isLoading = false;
  }

  private populateMap() {
    this.comparatorOptions.forEach((option) => {
      this.comparatorOptionsMap[option.name] = option.displayName;
    });
    this.isLoading = false;
  }

  private goTo(destination: string) {
    console.log(destination);
    this.location.go('/deployments?filter=' + destination);
  }

}