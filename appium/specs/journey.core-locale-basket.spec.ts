import { clearBasketIfNeeded, launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';
import { BasketPage } from '../pages/BasketPage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Journey — core locale FA basket labels', () => {
  before(async () => {
    await launchFresh();
    await clearBasketIfNeeded();
  });

  it('switches to FA and basket UI remains functional', async () => {
    const account = new AccountPage(browser);
    await account.open();
    await account.toggleLocaleFa();

    const products = new ProductsPage(browser);
    await products.open();
    await products.addFirstProductToBasket();

    const basket = new BasketPage(browser);
    await basket.open();
    await basket.expectLineItemsVisible();
    await basket.expectTotalVisible();
    await basket.expectProceedToCheckoutVisible();
  });
});
