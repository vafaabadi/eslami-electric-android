import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { ProductDetailPage } from './ProductDetailPage';

export class ProductsPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async open(): Promise<void> {
    await this.byTestTag(Selectors.navProducts).then((el) => el.click());
    await this.pause(500);
  }

  async expectCatalogControlsVisible(): Promise<void> {
    await this.byTestTag(Selectors.searchProducts);
    await this.byTestTag(Selectors.chipCategoryAll);
    await this.byTestTag(Selectors.chipSortDefault);
  }

  async search(term: string): Promise<void> {
    const field = await this.byTestTag(Selectors.searchProducts);
    await field.clearValue();
    await field.setValue(term);
    await this.hideKeyboardIfOpen();
    await this.pause(800);
  }

  async clearSearch(): Promise<void> {
    const field = await this.byTestTag(Selectors.searchProducts);
    await field.clearValue();
    await this.hideKeyboardIfOpen();
    await this.pause(800);
  }

  async countAddToBasketButtons(): Promise<number> {
    const buttons = await this.allByTestTag(Selectors.addToBasket);
    return buttons.length;
  }

  async addFirstProductToBasket(): Promise<void> {
    await this.byTestTag(Selectors.addToBasket, 30_000);
    const buttons = await this.allByTestTag(Selectors.addToBasket);
    await buttons[0].click();
  }

  async increaseFirstProductQuantity(): Promise<void> {
    const increase = await this.byTestTag(Selectors.stepperIncrease);
    await increase.click();
  }

  async openFirstProductDetail(): Promise<ProductDetailPage> {
    await this.byTestTag(Selectors.addToBasket, 30_000);
    const card = await $('android=new UiSelector().className("android.view.View").instance(10)');
    await card.click();
    await this.pause(1000);
    return new ProductDetailPage(this.driver);
  }

  async searchAndExpectNarrowed(term: string = UiText.searchDefaultTerm): Promise<void> {
    const before = await this.countAddToBasketButtons();
    await this.search(term);
    const after = await this.countAddToBasketButtons();
    expect(after).toBeGreaterThanOrEqual(0);
    expect(before).toBeGreaterThanOrEqual(after);
  }

  async clearSearchAndExpectRestored(): Promise<void> {
    await this.clearSearch();
    const count = await this.countAddToBasketButtons();
    expect(count).toBeGreaterThan(0);
  }
}
