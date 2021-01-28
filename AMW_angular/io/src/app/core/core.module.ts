import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

/**
 * The CoreModule loads modules that should be loaded only once.
 * Do not import the CoreModule in any modules other than the AppModule
 *
 * Place singleton services in the core module directory, but you don't need to provide them here,
 * since services itself declare where they should be provided, e.g.: <pre>providedIn: 'root'</pre>
 *
 * The core module should also not declare components, directives, pipes. Use the shared module instead.
 * Finally, this module should only be imported in the AppModule
 */
@NgModule({
  declarations: [],
  imports: [CommonModule, HttpClientModule],
})
export class CoreModule {}
