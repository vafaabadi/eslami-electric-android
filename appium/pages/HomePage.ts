import type { Browser } from 'webdriverio';
import { Selectors } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { ProductsPage } from './ProductsPage';

export class HomePage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async open(): Promise<void> {
    await this.byTestTag(Selectors.navHome).then((el) => el.click());
    await this.pause(500);
  }

  async expectFeaturedSectionVisible(): Promise<void> {
    const featured = await this.byTestTag(Selectors.homeFeatured, 30_000);
    await expect(featured).toBeDisplayed();
  }

  async expectAddToBasketVisible(): Promise<void> {
    const addBtn = await this.byTestTag(Selectors.addToBasket, 30_000);
    await expect(addBtn).toBeDisplayed();
  }

  async openViewAllProducts(): Promise<ProductsPage> {
    await (await this.byTestTag(Selectors.viewAllProducts)).click();
    await this.byTestTag(Selectors.searchProducts);
    return new ProductsPage(this.driver);
  }
}
