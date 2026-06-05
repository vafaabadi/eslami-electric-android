import { launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Product detail — add to basket from detail page', () => {
  before(async () => {
    await launchFresh();
    const basket = new BasketPage(browser);
    await basket.clearIfNeeded();
    const products = new ProductsPage(browser);
    await products.open();
  });

  it('adds item to basket from product detail screen', async () => {
    const products = new ProductsPage(browser);
    const detail = await products.openFirstProductDetail();
    await detail.addToBasket();

    const basket = new BasketPage(browser);
    await basket.open();
    await basket.expectLineItemsVisible();
  });
});
