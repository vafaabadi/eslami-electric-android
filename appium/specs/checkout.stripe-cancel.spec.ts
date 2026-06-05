import { launchFresh, openCheckoutAsGuest } from '../helpers/app';
import { CheckoutResultPage } from '../pages/CheckoutResultPage';

describe('Checkout — Stripe button opens and cancel returns', () => {
  before(async () => {
    await launchFresh();
  });

  it('opens Stripe checkout and returns without completing payment', async () => {
    const checkout = await openCheckoutAsGuest();
    await checkout.expectPayStripeVisible();
    await checkout.tapPayStripeAndCancel();

    const result = new CheckoutResultPage(browser);
    expect(await result.isIncompleteOrCheckoutVisible()).toBe(true);
  });
});
