import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SettingService } from './setting/setting.service';
import { AppConfiguration } from './setting/app-configuration';
import { AMW_LOGOUT_URL } from '@core/amw-constants';
import { NavigationItem } from '@core/services/navigation/navigation-item';
import { NavigationService } from '@core/services';

@Component({
  selector: 'app',
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./app.component.scss'],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnInit {
  logoutUrl: string;

  constructor(
    public navigationStore: NavigationService,
    private router: Router,
    private settingService: SettingService
  ) {}

  ngOnInit(): void {
    this.settingService.getAllAppSettings().subscribe((r) => this.configureSettings(r));
  }

  navigateTo(item: NavigationItem): void {
    this.navigationStore.setCurrent(item.title);
    this.router.navigateByUrl(item.target);
  }

  private configureSettings(settings: AppConfiguration[]) {
    const logoutUrl = settings.find((config) => config.key.value === AMW_LOGOUT_URL);
    this.logoutUrl = logoutUrl ? logoutUrl.value : '';
  }
}
