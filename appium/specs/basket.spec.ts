import { launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Basket — add via stepper and adjust quantity', () => {
  let basket: BasketPage;

  before(async () => {
    await launchFresh();
    const products = new ProductsPage(browser);
    await products.open();
    await products.increaseFirstProductQuantity();
    await products.addFirstProductToBasket();
    basket = new BasketPage(browser);
    await basket.open();
  });

  it('shows basket line items after add', async () => {
    await basket.expectLineItemsVisible();
  });

  it('increases quantity with stepper', async () => {
    await basket.increaseQuantity();
  });

  it('shows basket total', async () => {
    await basket.expectTotalVisible();
  });
});
