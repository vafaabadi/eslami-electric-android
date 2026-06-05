import { launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';
import { HomePage } from '../pages/HomePage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Journey — guest basket to checkout form', () => {
  before(async () => {
    await launchFresh();
    const basket = new BasketPage(browser);
    await basket.clearIfNeeded();
  });

  it('home → add product → basket → proceed → checkout guest fields', async () => {
    const home = new HomePage(browser);
    await home.open();
    await home.expectFeaturedSectionVisible();

    const products = await home.openViewAllProducts();
    await products.addFirstProductToBasket();

    const basket = new BasketPage(browser);
    await basket.open();
    await basket.expectLineItemsVisible();
    await basket.expectTotalVisible();
    await basket.expectProceedToCheckoutVisible();

    const checkout = await basket.proceedToCheckout();
    await checkout.expectCheckoutScreenVisible();
    await checkout.expectGuestCheckoutForm();
  });
});
