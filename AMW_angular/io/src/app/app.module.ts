import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgSelectModule } from '@ng-select/ng-select';
/*
 * Feature Modules
 */
import { ResourceModule } from './resource/resource.module';
import { PermissionModule } from './permission/permission.module';
import { AuditviewModule } from './auditview/auditview.module';
import { SettingModule } from './setting/setting.module';
import { BaseModule } from './base/base.module';
import { SharedModule } from '@shared/shared.module';
import { NavigationModule } from './navigation/navigation.module';
/*
 * Platform and Environment providers/directives/pipes
 */
import { AppRoutingModule } from './app-routing.module';
// App is our top level component
import { AppComponent } from './app.component';

import { DeploymentsModule } from './deployments/deployments.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import { CoreModule } from '@core/core.module';

@NgModule({
  declarations: [AppComponent],
  imports: [
    CoreModule,
    BrowserModule,
    BrowserAnimationsModule,
    NgSelectModule,
    AppRoutingModule,
    ResourceModule,
    DeploymentsModule,
    AuditviewModule,
    PermissionModule,
    SettingModule,
    BaseModule,
    SharedModule,
    NavigationModule,
    NgbModule,
    CodemirrorModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
  exports: [AppComponent],
})
export class AppModule {}
