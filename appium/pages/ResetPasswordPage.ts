import { $$ } from '@wdio/globals';
import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class ResetPasswordPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectResetPasswordVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.screenResetPassword, 10_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    }
    await this.byTextContains(UiText.resetPasswordTitle);
    expect(await this.textExists('new password')).toBe(true);
  }

  async expectFormFields(): Promise<void> {
    await this.byTextContains('Password');
    await this.byTextContains('Confirm password');
    await this.byTestTag(Selectors.resetPasswordSubmit);
  }

  async fillNewPassword(password: string): Promise<void> {
    const fields = await $$('android=new UiSelector().className("android.widget.EditText")');
    const passwordField = fields[fields.length - 2];
    await passwordField.setValue(password);
  }

  async fillConfirmPassword(password: string): Promise<void> {
    const fields = await $$('android=new UiSelector().className("android.widget.EditText")');
    const confirmField = fields[fields.length - 1];
    await confirmField.setValue(password);
  }

  async submit(): Promise<void> {
    await this.hideKeyboardIfOpen();
    await (await this.byTestTag(Selectors.resetPasswordSubmit)).click();
    await this.pause(1500);
  }
}
