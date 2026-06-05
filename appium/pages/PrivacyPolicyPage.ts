import { driver } from '@wdio/globals';
import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class PrivacyPolicyPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async tapPrivacyPolicyFromAccount(): Promise<void> {
    await (await this.byTestTag(Selectors.privacyPolicy)).click();
    await this.pause(2500);
  }

  async expectExternalViewOpened(): Promise<void> {
    const contexts = await driver.getContexts();
    const hasWebView = contexts.some(
      (ctx) => typeof ctx === 'string' && (ctx.includes('CHROMIUM') || ctx.includes('WEBVIEW'))
    );
    const hasChrome = await this.textExists('Chrome');
    const hasPrivacy = await this.textExists('Privacy');
    expect(hasWebView || hasChrome || hasPrivacy).toBe(true);
  }

  async returnToApp(): Promise<void> {
    await driver.back();
    await this.pause(1500);
  }

  async expectAccountScreenAfterBack(): Promise<void> {
    await this.byTestTag(Selectors.screenAccount, 15_000);
  }
}
