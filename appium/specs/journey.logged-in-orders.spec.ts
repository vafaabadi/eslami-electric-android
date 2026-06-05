import { launchFresh } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { AccountPage } from '../pages/AccountPage';

describe('Journey — logged-in my orders to order detail', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('login → my orders list → order detail items visible', async function () {
    const account = new AccountPage(browser);
    await account.open();
    const orders = await account.openMyOrders();
    await orders.expectScreenVisible();

    const hasOrders = await orders.hasOrders();
    if (!hasOrders) {
      this.skip();
      return;
    }

    const detail = await orders.openFirstOrder();
    await detail.expectOrderDetailVisible();
  });
});
