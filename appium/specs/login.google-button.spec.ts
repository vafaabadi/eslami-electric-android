import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Login — Google sign-in button visible', () => {
  before(async () => {
    await launchFresh();
  });

  it('shows Continue with Google without starting OAuth', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const login = await account.openLogin();
    await login.expectGoogleSignInVisible();
  });
});
