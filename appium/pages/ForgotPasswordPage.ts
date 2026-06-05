import type { Browser } from 'webdriverio';
import { UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class ForgotPasswordPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectForgotPasswordVisible(): Promise<void> {
    await this.byTextContains(UiText.forgotPasswordTitle, 10_000);
  }

  async expectHintAndFields(): Promise<void> {
    expect(await this.textExists(UiText.forgotPasswordTitle)).toBe(true);
    expect(await this.textExists('reset link')).toBe(true);
    await this.byTextContains('Email');
    await this.byTextContains(UiText.sendResetLink);
  }
}
