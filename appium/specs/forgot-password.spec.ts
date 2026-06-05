import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Account — forgot password screen', () => {
  let forgot: import('../pages/ForgotPasswordPage').ForgotPasswordPage;

  before(async () => {
    await launchFresh();
    const account = new AccountPage(browser);
    await account.open();
    const login = await account.openLogin();
    forgot = await login.openForgotPassword();
  });

  it('shows forgot password title and hint', async () => {
    await forgot.expectHintAndFields();
  });
});
