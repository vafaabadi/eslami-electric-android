import { launchFresh, openCheckoutResultDeepLink } from '../helpers/app';

describe('Checkout — success result via deep link', () => {
  before(async () => {
    await launchFresh();
  });

  it('shows payment successful screen from adb deep link', async () => {
    const result = await openCheckoutResultDeepLink({
      success: true,
      orderNumber: 'ORD-E2E-TEST',
      orderId: 'e2e-order-id',
      guestToken: 'e2e-guest-token',
    });
    await result.expectSuccessVisible();
  });
});
