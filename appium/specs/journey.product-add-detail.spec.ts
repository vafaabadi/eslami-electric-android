import { launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Journey — product detail add quantity to basket', () => {
  before(async () => {
    await launchFresh();
    const basket = new BasketPage(browser);
    await basket.clearIfNeeded();
    const products = new ProductsPage(browser);
    await products.open();
  });

  it('products → detail → add qty 2 → basket shows quantity 2', async () => {
    const products = new ProductsPage(browser);
    const detail = await products.openFirstProductDetail();
    await detail.expectVisible();
    await detail.addQuantity(2);

    const basket = new BasketPage(browser);
    await basket.open();
    await basket.expectLineItemsVisible();
    await basket.expectQuantityAtLeast(2);
  });
});
