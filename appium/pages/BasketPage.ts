import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { CheckoutPage } from './CheckoutPage';
import { NavigationBar } from './NavigationBar';
import { ProductsPage } from './ProductsPage';

export class BasketPage extends BasePage {
  private readonly nav: NavigationBar;

  constructor(driver: Browser) {
    super(driver);
    this.nav = new NavigationBar(driver);
  }

  async open(): Promise<void> {
    await this.nav.tapBasket();
  }

  async clearIfNeeded(): Promise<void> {
    await this.open();
    for (let i = 0; i < 20; i++) {
      const empty = await this.byTestTagIfExists(Selectors.basketEmpty, 2_000);
      if (empty) return;
      const remove = await this.byTextExact(UiText.basketRemove, 2_000).catch(() => null);
      if (!remove) return;
      await remove.click();
      await this.pause(500);
    }
  }

  async ensureHasItem(): Promise<void> {
    await this.open();
    const hasLine = await this.byTestTagIfExists(Selectors.basketLineItem, 3_000);
    if (hasLine) return;

    const products = new ProductsPage(this.driver);
    await products.open();
    await products.addFirstProductToBasket();
    await this.open();
    await this.byTestTag(Selectors.basketLineItem, 15_000);
  }

  async expectEmptyState(): Promise<void> {
    const empty = await this.byTestTag(Selectors.basketEmpty, 10_000);
    await expect(empty).toBeDisplayed();
    const text = await empty.getText();
    expect(text.toLowerCase()).toContain('empty');
  }

  async expectLineItemsVisible(): Promise<void> {
    const line = await this.byTestTag(Selectors.basketLineItem, 15_000);
    await expect(line).toBeDisplayed();
  }

  async increaseQuantity(): Promise<void> {
    const qtyBefore = await (await this.byTestTag(Selectors.stepperQuantity)).getText();
    await (await this.byTestTag(Selectors.stepperIncrease)).click();
    await this.pause(400);
    const qtyAfter = await (await this.byTestTag(Selectors.stepperQuantity)).getText();
    expect(Number(qtyAfter)).toBeGreaterThan(Number(qtyBefore));
  }

  async expectTotalVisible(): Promise<void> {
    const total = await this.byTestTag(Selectors.basketTotal);
    await expect(total).toBeDisplayed();
    const text = await total.getText();
    expect(text).toMatch(/\$/);
  }

  async expectProceedToCheckoutVisible(): Promise<void> {
    const btn = await this.byTestTag(Selectors.checkoutProceed, 10_000).catch(async () => {
      return this.byTextContains(UiText.checkoutProceed);
    });
    await expect(btn).toBeDisplayed();
  }

  async proceedToCheckout(): Promise<CheckoutPage> {
    const tagged = await this.byTestTagIfExists(Selectors.checkoutProceed, 3_000);
    if (tagged) {
      await tagged.click();
    } else {
      await (await this.byTextContains(UiText.checkoutProceed)).click();
    }
    await this.pause(800);
    const checkout = new CheckoutPage(this.driver);
    await checkout.expectCheckoutScreenVisible();
    return checkout;
  }
}
