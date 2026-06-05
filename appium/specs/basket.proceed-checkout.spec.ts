import { launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';

describe('Basket — proceed to checkout CTA', () => {
  before(async () => {
    await launchFresh();
    const basket = new BasketPage(browser);
    await basket.ensureHasItem();
    await basket.open();
  });

  it('shows Proceed to checkout when basket has items', async () => {
    const basket = new BasketPage(browser);
    await basket.expectProceedToCheckoutVisible();
  });

  it('navigates to checkout screen from basket CTA', async () => {
    const basket = new BasketPage(browser);
    const checkout = await basket.proceedToCheckout();
    await checkout.expectCheckoutScreenVisible();
  });
});
