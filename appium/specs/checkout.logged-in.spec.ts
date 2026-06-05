import { launchFresh, openCheckoutAsGuest } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';

describe('Checkout — logged-in user reaches checkout form', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('shows checkout screen without guest-only section', async () => {
    const checkout = await openCheckoutAsGuest();
    await checkout.expectLoggedInCheckoutForm();
  });
});
