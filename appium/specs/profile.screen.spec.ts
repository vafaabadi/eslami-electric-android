import { launchFresh } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { AccountPage } from '../pages/AccountPage';

describe('Account — profile view and edit fields', () => {
  let profile: import('../pages/ProfilePage').ProfilePage;

  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
    const account = new AccountPage(browser);
    await account.open();
    profile = await account.openProfile();
  });

  it('shows profile screen title', async () => {
    await profile.expectProfileVisible();
  });

  it('shows editable profile fields', async () => {
    await profile.expectEditableFields();
  });
});
