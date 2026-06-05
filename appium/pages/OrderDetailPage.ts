import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { BasketPage } from './BasketPage';

export class OrderDetailPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectScreenVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.screenOrderDetail, 10_000);
    if (screen) {
      await expect(screen).toBeDisplayed();
    }
    expect(await this.textExists(UiText.orderDetailTitle)).toBe(true);
  }

  async expectOrderDetailVisible(): Promise<void> {
    await this.expectScreenVisible();
    await this.byTextContains(UiText.orderItemsHeading);
  }

  async hasEditBeforePayment(): Promise<boolean> {
    const btn = await this.byTestTagIfExists(Selectors.orderEditBeforePayment, 5_000);
    return btn !== null;
  }

  async expectEditBeforePaymentVisible(): Promise<void> {
    const btn = await this.byTestTag(Selectors.orderEditBeforePayment, 10_000);
    await expect(btn).toBeDisplayed();
  }

  async tapEditBeforePayment(): Promise<BasketPage> {
    await (await this.byTestTag(Selectors.orderEditBeforePayment)).click();
    await this.pause(1500);
    const basket = new BasketPage(this.driver);
    await basket.expectEditPendingBanner();
    return basket;
  }
}
