import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Account — login screen navigation', () => {
  before(async () => {
    await launchFresh();
  });

  it('opens login screen from Account tab', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const login = await account.openLogin();
    await login.expectLoginFormVisible();
  });
});
