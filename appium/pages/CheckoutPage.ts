import { driver } from '@wdio/globals';
import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class CheckoutPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectCheckoutScreenVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.checkoutScreen, 5_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    } else {
      await this.byTextContains(UiText.checkoutTitle, 15_000);
    }
  }

  async expectGuestCheckoutForm(): Promise<void> {
    expect(await this.textExists(UiText.checkoutTitle)).toBe(true);
    expect(await this.textExists(UiText.checkoutDelivery)).toBe(true);
    expect(await this.textExists(UiText.checkoutCollection)).toBe(true);
    expect(await this.textExists(UiText.checkoutGuestDetails)).toBe(true);
    expect(await this.textExists(UiText.checkoutShippingAddress)).toBe(true);
    await this.byTextContains('Full name');
    await this.byTextContains('Email');
    await this.byTextContains('Street address');
  }

  async expectLoggedInCheckoutForm(): Promise<void> {
    expect(await this.textExists(UiText.checkoutTitle)).toBe(true);
    expect(await this.textExists(UiText.checkoutGuestDetails)).toBe(false);
    expect(await this.textExists(UiText.checkoutShippingAddress)).toBe(true);
    expect(await this.textExists(UiText.checkoutPayStripe)).toBe(true);
  }

  async expectPayStripeVisible(): Promise<void> {
    const btn = await this.byTestTagIfExists(Selectors.checkoutPayStripe, 5_000);
    if (btn) {
      await expect(btn).toBeDisplayed();
    } else {
      expect(await this.textExists(UiText.checkoutPayStripe)).toBe(true);
    }
  }

  async tapPayStripeAndCancel(): Promise<void> {
    const tagged = await this.byTestTagIfExists(Selectors.checkoutPayStripe, 3_000);
    if (tagged) {
      await tagged.click();
    } else {
      await (await this.byTextContains(UiText.checkoutPayStripe)).click();
    }
    await this.pause(2000);
    await driver.back();
    await this.pause(1500);
  }
}
