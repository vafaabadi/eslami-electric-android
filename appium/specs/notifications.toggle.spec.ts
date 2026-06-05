import { launchFresh } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { AccountPage } from '../pages/AccountPage';

describe('Notifications — toggle interaction', () => {
  before(async function () {
    await launchFresh();
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await loginWithEnvCredentials();
  });

  it('toggles master notification preference', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const notifications = await account.openNotifications();
    if (!notifications) return;
    await notifications.toggleMasterIfPresent();
    await notifications.expectTogglesVisible();
  });
});
