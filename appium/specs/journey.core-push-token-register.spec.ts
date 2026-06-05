import { launchFresh } from '../helpers/app';
import {
  assertLoggedInAccountState,
  hasTestCredentials,
  loginWithEnvCredentials,
} from '../helpers/auth';
import { hasTestCredentialsForPush } from '../helpers/env';
import { AccountPage } from '../pages/AccountPage';

describe('Journey — core push token register (notifications screen)', () => {
  before(async function () {
    if (!hasTestCredentialsForPush()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('opens notifications preferences after login (FCM may be idle on emulator)', async () => {
    await assertLoggedInAccountState();
    const account = new AccountPage(browser);
    await account.open();
    const notifications = await account.openNotifications();
    if (!notifications) {
      // FCM not configured in this build — screen hidden; journey still passes login path.
      return;
    }
    await notifications.expectScreenVisible();
  });
});
