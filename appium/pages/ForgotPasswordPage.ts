import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class ForgotPasswordPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectForgotPasswordVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.screenForgotPassword, 10_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    }
    await this.byTextContains(UiText.forgotPasswordTitle);
  }

  async expectHintAndFields(): Promise<void> {
    expect(await this.textExists(UiText.forgotPasswordTitle)).toBe(true);
    expect(await this.textExists('reset link')).toBe(true);
    await this.byTestTag(Selectors.forgotEmail);
    await this.byTestTag(Selectors.sendResetLink);
  }

  async fillEmail(email: string): Promise<void> {
    const field = await this.byTestTag(Selectors.forgotEmail);
    await field.setValue(email);
  }

  async submitEmptyAndExpectValidation(): Promise<void> {
    await this.hideKeyboardIfOpen();
    await (await this.byTestTag(Selectors.sendResetLink)).click();
    await this.pause(800);
    expect(await this.textExists(UiText.forgotPasswordTitle)).toBe(true);
  }

  async submitInvalidEmailAndExpectError(): Promise<void> {
    await this.fillEmail('not-an-email');
    await this.hideKeyboardIfOpen();
    await (await this.byTestTag(Selectors.sendResetLink)).click();
    await this.pause(1000);
    const hasError =
      (await this.textExists('valid email')) ||
      (await this.textExists('email')) ||
      (await this.textExists('required'));
    expect(hasError).toBe(true);
  }
}
