import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class NotificationsPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectScreenVisible(): Promise<void> {
    await this.byTestTag(Selectors.notificationsScreen, 20_000);
  }

  async expectTogglesVisible(): Promise<void> {
    const master = await this.byTestTagIfExists(Selectors.notificationsMaster, 20_000);
    if (master) {
      await expect(master).toBeDisplayed();
    }
    const orders = await this.byTestTagIfExists(Selectors.notificationsOrders, 10_000);
    if (orders) {
      await expect(orders).toBeDisplayed();
    }
  }

  async toggleMasterIfPresent(): Promise<void> {
    const master = await this.byTestTagIfExists(Selectors.notificationsMaster, 10_000);
    if (!master) return;
    await master.click();
    await this.pause(600);
    await master.click();
    await this.pause(600);
  }
}
