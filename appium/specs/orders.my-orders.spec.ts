import { launchFresh } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { AccountPage } from '../pages/AccountPage';

describe('Orders — my orders (authenticated)', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('opens My orders from Account', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const orders = await account.openMyOrders();
    await orders.expectScreenVisible();
  });
});
