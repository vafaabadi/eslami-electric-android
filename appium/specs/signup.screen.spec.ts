import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Account — sign up screen fields', () => {
  let signUp: import('../pages/SignUpPage').SignUpPage;

  before(async () => {
    await launchFresh();
    const account = new AccountPage(browser);
    await account.open();
    const login = await account.openLogin();
    signUp = await login.openSignUp();
  });

  it('shows create account title', async () => {
    await signUp.expectSignUpFormVisible();
  });

  it('shows person and company account type chips', async () => {
    await signUp.expectAccountTypeChips();
  });

  it('shows required signup form fields', async () => {
    await signUp.expectRequiredFields();
  });
});
