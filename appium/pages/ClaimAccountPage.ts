import { $$ } from '@wdio/globals';
import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class ClaimAccountPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectClaimAccountVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.screenClaimAccount, 10_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    }
    await this.byTextContains(UiText.claimAccountTitle);
  }

  async expectTokenFieldAndValidate(): Promise<void> {
    await this.byTestTag(Selectors.claimToken);
    await this.byTestTag(Selectors.claimValidate);
    expect(await this.textExists(UiText.claimValidate)).toBe(true);
  }

  async fillToken(token: string): Promise<void> {
    const field = await this.byTestTag(Selectors.claimToken);
    await field.setValue(token);
  }

  async tapValidate(): Promise<void> {
    await this.hideKeyboardIfOpen();
    await (await this.byTestTag(Selectors.claimValidate)).click();
    await this.pause(2000);
  }

  async fillPasswords(password: string): Promise<void> {
    const all = await $$('android=new UiSelector().className("android.widget.EditText")');
    const count = all.length;
    if (count >= 3) {
      await all[count - 2].setValue(password);
      await all[count - 1].setValue(password);
    }
  }

  async submitClaim(): Promise<void> {
    await this.hideKeyboardIfOpen();
    await (await this.byTestTag(Selectors.claimAccountSubmit)).click();
    await this.pause(2000);
  }

  async expectValidationErrorOrReady(): Promise<void> {
    const hasEmail = await this.textExists('Email on order');
    const hasError = await this.textExists('Invalid') || (await this.textExists('expired'));
    expect(hasEmail || hasError).toBe(true);
  }
}
