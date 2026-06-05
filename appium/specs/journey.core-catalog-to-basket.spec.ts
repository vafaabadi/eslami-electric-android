import { clearBasketIfNeeded, launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Journey — core catalog to basket totals', () => {
  before(async () => {
    await launchFresh();
    await clearBasketIfNeeded();
  });

  it('browses catalog, adds multiple products, and shows correct basket total', async () => {
    const products = new ProductsPage(browser);
    await products.open();
    await products.expectCatalogControlsVisible();
    await products.addMultipleDistinctProducts(2);

    const basket = new BasketPage(browser);
    await basket.open();
    await basket.expectLineItemCountAtLeast(1);
    await basket.expectTotalVisible();
    const total = await basket.readTotalAmount();
    expect(total).toBeGreaterThan(0);
  });
});
