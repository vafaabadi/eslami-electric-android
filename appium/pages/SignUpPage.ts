import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class SignUpPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectSignUpFormVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.signupScreen, 5_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    } else {
      expect(await this.textExists(UiText.signupTitle)).toBe(true);
    }
  }

  async expectAccountTypeChips(): Promise<void> {
    expect(await this.textExists('Person')).toBe(true);
    expect(await this.textExists('Company')).toBe(true);
  }

  async expectRequiredFields(): Promise<void> {
    await this.byTextContains(UiText.firstName);
    await this.byTextContains('Surname');
    await this.byTextContains('Email');
    await this.byTextContains('Address');
    await this.byTextContains('Password');
    await this.byTextContains(UiText.signUp);
  }
}
