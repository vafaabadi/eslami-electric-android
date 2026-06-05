import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class GuestTrackOrderPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectEmailTabVisible(): Promise<void> {
    await this.byTestTag(Selectors.guestEmail);
    await this.byTestTag(Selectors.guestOrderRef);
  }

  async switchToTokenTab(): Promise<void> {
    await (await this.byTestTag(Selectors.guestTrackModeToken)).click();
    await this.byTestTag(Selectors.guestToken);
  }

  async switchToEmailTab(): Promise<void> {
    await (await this.byTestTag(Selectors.guestTrackModeEmail)).click();
    await this.byTestTag(Selectors.guestEmail);
  }

  async submitEmptyEmailLookup(): Promise<void> {
    await (await this.byTestTag(Selectors.guestTrackSubmit)).click();
    await this.hideKeyboardIfOpen();
  }

  async expectEmailValidationError(): Promise<void> {
    const error = await $('android=new UiSelector().textContains("email")');
    expect(await error.isExisting()).toBe(true);
  }

  async pasteOrderNumberOnTokenTab(orderRef: string): Promise<void> {
    await this.switchToTokenTab();
    const tokenField = await this.byTestTag(Selectors.guestToken);
    await tokenField.setValue(orderRef);
    await (await this.byTestTag(Selectors.guestTrackSubmit)).click();
    await this.pause(600);
  }

  async expectSwitchedToEmailTabWithOrderRef(orderRef: string): Promise<void> {
    await this.byTestTag(Selectors.guestOrderRef);
    await this.byTestTag(Selectors.guestEmail);
    const orderRefField = await this.byTestTag(Selectors.guestOrderRef);
    const value = await orderRefField.getText();
    expect(value.toUpperCase()).toContain(orderRef.toUpperCase());
  }

  async submitInvalidToken(): Promise<void> {
    await this.switchToTokenTab();
    const tokenField = await this.byTestTag(Selectors.guestToken);
    await tokenField.setValue('invalid-token-xyz');
    await (await this.byTestTag(Selectors.guestTrackSubmit)).click();
    await this.pause(1200);
  }

  async expectTokenLookupError(): Promise<void> {
    const error = await $('android=new UiSelector().textContains("order")');
    const notFound = await $('android=new UiSelector().textContains("not found")');
    const invalid = await $('android=new UiSelector().textContains("invalid")');
    const hasError =
      (await error.isExisting()) || (await notFound.isExisting()) || (await invalid.isExisting());
    expect(hasError).toBe(true);
  }
}
