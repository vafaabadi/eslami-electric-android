import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class CheckoutResultPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectSuccessVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.checkoutResultScreen, 10_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    }
    expect(await this.textExists('Payment successful')).toBe(true);
    expect(await this.textExists('Thank you')).toBe(true);
  }

  async expectIncompleteVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.checkoutResultScreen, 10_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    }
    expect(
      (await this.textExists('Payment not completed')) ||
        (await this.textExists('not completed'))
    ).toBe(true);
  }

  async tapDone(): Promise<void> {
    await (await this.byTextContains(UiText.checkoutDone)).click();
    await this.pause(800);
  }

  async expectClaimAccountCtaVisible(): Promise<void> {
    const btn = await this.byTestTagIfExists(Selectors.checkoutClaimAccount, 10_000);
    if (btn) {
      await expect(btn).toBeDisplayed();
      return;
    }
    expect(await this.textExists(UiText.checkoutClaimAccount)).toBe(true);
    expect(await this.textExists(UiText.checkoutClaimHint)).toBe(true);
  }

  async isIncompleteOrCheckoutVisible(): Promise<boolean> {
    return (
      (await this.textExists('Payment not completed')) ||
      (await this.textExists('not completed')) ||
      (await this.textExists('Checkout'))
    );
  }
}
