import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class ProfilePage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectProfileVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.profileScreen, 5_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    } else {
      expect(await this.textExists(UiText.profileTitle)).toBe(true);
    }
  }

  async expectEditableFields(): Promise<void> {
    await this.byTextContains(UiText.firstName);
    await this.byTextContains('Surname');
    await this.byTextContains('Mobile');
    await this.byTextContains('Address');
    const save = await this.byTestTagIfExists(Selectors.saveProfile, 3_000);
    if (save) {
      await expect(save).toBeDisplayed();
    } else {
      await this.byTextContains(UiText.saveProfile);
    }
  }
}
