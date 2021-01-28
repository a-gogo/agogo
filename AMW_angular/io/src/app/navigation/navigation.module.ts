import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationComponent } from './navigation.component';
import { NavigationSidebarComponent } from './navigation-sidebar.component';
import { SharedModule } from '@shared/shared.module';

@NgModule({
  imports: [SharedModule, CommonModule],
  declarations: [NavigationComponent, NavigationSidebarComponent],
  exports: [NavigationComponent, NavigationSidebarComponent],
})
export class NavigationModule {}
