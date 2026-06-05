import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class ProductDetailPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectVisible(): Promise<void> {
    const detail = await this.byTestTagIfExists(Selectors.productDetail, 10_000);
    if (detail) {
      await expect(detail).toBeDisplayed();
      await this.byTestTag(Selectors.addToBasket);
      return;
    }
    const title = await $('android=new UiSelector().textContains("Product")');
    expect(await title.isExisting()).toBe(true);
  }

  async expectAddToBasketVisible(): Promise<void> {
    await expect(this.byTestTag(Selectors.addToBasket)).resolves.toBeDefined();
  }

  async addToBasket(): Promise<void> {
    await (await this.byTestTag(Selectors.addToBasket, 15_000)).click();
    await this.pause(600);
  }
}
