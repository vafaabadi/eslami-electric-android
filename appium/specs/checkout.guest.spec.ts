import { launchFresh, openCheckoutAsGuest } from '../helpers/app';

describe('Checkout — guest basket to checkout form', () => {
  before(async () => {
    await launchFresh();
  });

  it('shows guest checkout form fields', async () => {
    const checkout = await openCheckoutAsGuest();
    await checkout.expectGuestCheckoutForm();
  });

  it('shows Pay with Stripe button without submitting payment', async () => {
    const checkout = await openCheckoutAsGuest();
    await checkout.expectPayStripeVisible();
  });
});
