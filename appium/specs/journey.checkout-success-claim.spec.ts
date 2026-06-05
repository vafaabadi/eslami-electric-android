import { launchFresh, openCheckoutResultDeepLink } from '../helpers/app';

describe('Journey — checkout success claim account CTA', () => {
  before(async () => {
    await launchFresh();
  });

  it('guest checkout success shows create-account-from-order CTA via deep link', async () => {
    const result = await openCheckoutResultDeepLink({
      success: true,
      orderNumber: 'ORD-E2E-CLAIM',
      orderId: 'e2e-claim-order-id',
      guestToken: 'e2e-guest-claim-token',
    });
    await result.expectSuccessVisible();
    await result.expectClaimAccountCtaVisible();
  });
});
