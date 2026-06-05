import { driver } from '@wdio/globals';
import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { NavigationBar } from './NavigationBar';

export class WhatsAppSupportPage extends BasePage {
  private readonly nav: NavigationBar;

  constructor(driver: Browser) {
    super(driver);
    this.nav = new NavigationBar(driver);
  }

  async expectFabVisibleOnHome(): Promise<void> {
    await this.nav.tapHome();
    const fab = await this.byTestTag(Selectors.whatsappFab, 10_000);
    await expect(fab).toBeDisplayed();
  }

  async tapFabWithoutCrash(): Promise<void> {
    await (await this.byTestTag(Selectors.whatsappFab)).click();
    await this.pause(2500);
    await this.returnToAppIfExternal();
    await this.nav.expectReady();
  }

  async expectAccountWhatsAppButtonVisible(): Promise<void> {
    const btn = await this.byTestTag(Selectors.contactWhatsapp, 10_000);
    await expect(btn).toBeDisplayed();
  }

  async tapAccountWhatsAppWithoutCrash(): Promise<void> {
    await (await this.byTestTag(Selectors.contactWhatsapp)).click();
    await this.pause(2500);
    await this.returnToAppIfExternal();
    await this.byTestTag(Selectors.screenAccount, 10_000);
  }

  private async returnToAppIfExternal(): Promise<void> {
    try {
      const pkg = await driver.getCurrentPackage();
      if (pkg !== 'com.eslamielectric.android') {
        await driver.back();
        await this.pause(1000);
      }
    } catch {
      await driver.back();
      await this.pause(1000);
    }
  }
}
