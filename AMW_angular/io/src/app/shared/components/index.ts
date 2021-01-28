import { DateTimePickerComponent } from './date-time-picker/date-time-picker.component';
import { SortableIconComponent } from './sortable-icon/sortable-icon.component';
import { LoadingIndicatorComponent } from './loading-indicator/loading-indicator.component';
import { PaginationComponent } from './pagination/pagination.component';
import { NotificationComponent } from './notification/notification.component';
import { IconComponent } from './icon/icon.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';

// used to import all components in SharedModule
export const components: any[] = [
  DateTimePickerComponent,
  IconComponent,
  SortableIconComponent,
  LoadingIndicatorComponent,
  PageNotFoundComponent,
  PaginationComponent,
  NotificationComponent,
];

/**
 * Export all components to simplify imports from anywhere -just import the shared components like this:
 * import { IconComponent } from '@shared/components';
 */
export * from './date-time-picker/date-time-picker.component';
export * from './icon/icon.component';
export * from './sortable-icon/sortable-icon.component';
export * from './loading-indicator/loading-indicator.component';
export * from './page-not-found/page-not-found.component';
export * from './pagination/pagination.component';
export * from './notification/notification.component';

/**
 * export shared types - TODO: check if this is a good way or if types should go somewhere else.
 */
export * from './date-time-picker/date-time.model';
