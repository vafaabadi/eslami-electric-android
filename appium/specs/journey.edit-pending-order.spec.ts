import {
  launchFresh,
  openOrderDetailDeepLink,
} from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { hasTestPendingOrderId } from '../helpers/env';
import { AccountPage } from '../pages/AccountPage';

describe('Journey — edit pending order before payment', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('loads pending order and edits basket before checkout (skip Stripe)', async function () {
    let detail;
    if (hasTestPendingOrderId()) {
      detail = await openOrderDetailDeepLink(process.env.TEST_PENDING_ORDER_ID!);
    } else {
      const account = new AccountPage(browser);
      await account.open();
      const orders = await account.openMyOrders();
      const hasOrders = await orders.hasOrders();
      if (!hasOrders) {
        this.skip();
        return;
      }
      detail = await orders.openFirstOrder();
    }

    await detail.expectScreenVisible();
    if (!(await detail.hasEditBeforePayment())) {
      this.skip();
      return;
    }

    const basket = await detail.tapEditBeforePayment();
    await basket.expectLineItemsVisible();
    const checkout = await basket.proceedToCheckout();
    await checkout.expectCheckoutScreenVisible();
    await checkout.expectLoggedInCheckoutForm();
  });
});
