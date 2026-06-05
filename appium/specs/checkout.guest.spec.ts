import { byTextContains, dismissSystemDialogs, openCheckoutAsGuest, textExists, waitForAppReady } from '../helpers/app';
import { UiText } from '../helpers/selectors';

describe('Checkout — guest basket to checkout form', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await openCheckoutAsGuest();
  });

  it('shows checkout screen title', async () => {
    expect(await textExists(UiText.checkoutTitle)).toBe(true);
  });

  it('shows fulfillment options', async () => {
    expect(await textExists(UiText.checkoutDelivery)).toBe(true);
    expect(await textExists(UiText.checkoutCollection)).toBe(true);
  });

  it('shows guest detail and shipping address fields', async () => {
    expect(await textExists(UiText.checkoutGuestDetails)).toBe(true);
    expect(await textExists(UiText.checkoutShippingAddress)).toBe(true);
    await expect(byTextContains('Full name')).resolves.toBeDefined();
    await expect(byTextContains('Email')).resolves.toBeDefined();
    await expect(byTextContains('Street address')).resolves.toBeDefined();
  });

  it('shows Pay with Stripe button without submitting payment', async () => {
    expect(await textExists(UiText.checkoutPayStripe)).toBe(true);
  });
});
