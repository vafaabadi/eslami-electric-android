import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';

export type NavTab = 'navHome' | 'navProducts' | 'navBasket' | 'navAccount';

export class NavigationBar extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async tap(tab: NavTab): Promise<void> {
    const el = await this.byTestTag(Selectors[tab]);
    await el.click();
    await this.pause(500);
  }

  async tapHome(): Promise<void> {
    await this.tap('navHome');
  }

  async tapProducts(): Promise<void> {
    await this.tap('navProducts');
  }

  async tapBasket(): Promise<void> {
    await this.tap('navBasket');
  }

  async tapAccount(): Promise<void> {
    await this.tap('navAccount');
  }

  async expectReady(timeoutMs = 30_000): Promise<void> {
    await this.byTestTag(Selectors.navHome, timeoutMs);
  }

  async expectAllTabsVisible(): Promise<void> {
    await this.byTestTag(Selectors.navHome);
    await this.byTestTag(Selectors.navProducts);
    await this.byTestTag(Selectors.navBasket);
    await this.byTestTag(Selectors.navAccount);
  }
}
