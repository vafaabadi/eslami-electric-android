import { launchFresh } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { AccountPage } from '../pages/AccountPage';

describe('Orders — order detail from my orders list', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('opens first order detail when orders exist', async function () {
    const account = new AccountPage(browser);
    await account.open();
    const orders = await account.openMyOrders();
    const hasOrders = await orders.hasOrders();
    if (!hasOrders) {
      this.skip();
      return;
    }
    const detail = await orders.openFirstOrder();
    await detail.expectOrderDetailVisible();
  });
});
