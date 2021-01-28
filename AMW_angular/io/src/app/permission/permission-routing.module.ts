import { RouterModule } from '@angular/router';
import { PermissionComponent } from './permission.component';
import { NgModule } from '@angular/core';
import { PageNotFoundComponent } from '../shared/components';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'permission/delegation/:actingUser',
        component: PermissionComponent,
      },
      { path: 'permission/:restrictionType', component: PermissionComponent },
      { path: 'permission', component: PermissionComponent },
      { path: '**', component: PageNotFoundComponent },
    ]),
  ],
  exports: [RouterModule],
})
export class PermissionRoutingModule {}
