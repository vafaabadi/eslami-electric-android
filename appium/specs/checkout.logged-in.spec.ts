import { dismissSystemDialogs, openCheckoutAsGuest, textExists, waitForAppReady } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { UiText } from '../helpers/selectors';

describe('Checkout — logged-in user reaches checkout form', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await dismissSystemDialogs();
    await waitForAppReady();
    await loginWithEnvCredentials();
    await openCheckoutAsGuest();
  });

  it('shows checkout screen without guest-only section', async () => {
    expect(await textExists(UiText.checkoutTitle)).toBe(true);
    expect(await textExists(UiText.checkoutGuestDetails)).toBe(false);
  });

  it('shows delivery address fields for logged-in checkout', async () => {
    expect(await textExists(UiText.checkoutShippingAddress)).toBe(true);
    expect(await textExists(UiText.checkoutPayStripe)).toBe(true);
  });
});
