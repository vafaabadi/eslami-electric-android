import { launchFresh } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { AccountPage } from '../pages/AccountPage';

describe('Orders — my orders empty state', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('shows empty state when user has no orders', async function () {
    const account = new AccountPage(browser);
    await account.open();
    const orders = await account.openMyOrders();
    const hasOrders = await orders.hasOrders();
    if (hasOrders) {
      this.skip();
      return;
    }
    await orders.expectEmptyState();
  });
});
