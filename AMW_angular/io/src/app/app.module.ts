import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { NgSelectModule } from '@ng-select/ng-select';
/*
 * Feature Modules
 */
import { ResourceModule } from './resource/resource.module';
import { DeploymentModule } from './deployment/deployment.module';
import { PermissionModule } from './permission/permission.module';
import { AuditviewModule } from './auditview/auditview.module';
import { SettingModule } from './setting/setting.module';
import { BaseModule } from './base/base.module';
import { SharedModule } from './shared/shared.module';
import { NavigationModule } from './navigation/navigation.module';
/*
 * Platform and Environment providers/directives/pipes
 */
import { AppRoutingModule } from './app-routing.module';
// App is our top level component
import { AppComponent } from './app.component';

import { DeploymentsModule } from './deployments/deployments.module';
import { NavigationStoreService } from './navigation/navigation-store.service';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

// Application wide providers
const APP_PROVIDERS = [NavigationStoreService];

import {
  FontAwesomeModule,
  FaIconLibrary,
} from '@fortawesome/angular-fontawesome';
import { faCalendar, faClock } from '@fortawesome/free-regular-svg-icons';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    NgSelectModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
    ResourceModule,
    DeploymentModule,
    DeploymentsModule,
    AuditviewModule,
    PermissionModule,
    SettingModule,
    BaseModule,
    SharedModule,
    NavigationModule,
    FontAwesomeModule,
    NgbModule,
  ],
  providers: [APP_PROVIDERS],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor(library: FaIconLibrary) {
    library.addIcons(faCalendar, faClock);
  }
}
