import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { OrderDetailPage } from './OrderDetailPage';

export class MyOrdersPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectScreenVisible(): Promise<void> {
    const screen = await this.byTestTagIfExists(Selectors.myOrdersScreen, 5_000);
    if (!screen) {
      expect(await this.textExists(UiText.myOrders)).toBe(true);
    }
  }

  async expectEmptyState(): Promise<void> {
    const empty = await this.byTestTagIfExists(Selectors.ordersEmpty, 10_000);
    if (empty) {
      await expect(empty).toBeDisplayed();
      return;
    }
    expect(await this.textExists('You have no orders yet')).toBe(true);
  }

  async hasOrders(): Promise<boolean> {
    if (await this.textExists('You have no orders yet')) return false;
    const empty = await this.byTestTagIfExists(Selectors.ordersEmpty, 3_000);
    if (empty) return false;
    const orderCard = await $('android=new UiSelector().textMatches("ORD-.*")');
    return orderCard.isExisting();
  }

  async openFirstOrder(): Promise<OrderDetailPage> {
    const orderCard = await $('android=new UiSelector().textMatches("ORD-.*")');
    await orderCard.waitForExist({ timeout: 15_000 });
    await orderCard.click();
    await this.pause(1200);
    return new OrderDetailPage(this.driver);
  }

  async findEditBeforePaymentOnList(): Promise<boolean> {
    const editBtn = await this.byTestTagIfExists(Selectors.orderEditBeforePayment, 5_000);
    return editBtn !== null;
  }
}
