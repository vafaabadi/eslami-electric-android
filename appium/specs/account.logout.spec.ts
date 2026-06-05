import { launchFresh } from '../helpers/app';
import {
  assertGuestAccountState,
  assertLoggedInAccountState,
  hasTestCredentials,
  loginWithEnvCredentials,
  logoutIfLoggedIn,
} from '../helpers/auth';

describe('Account — logout returns to guest state', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('shows logged-in account actions before logout', async () => {
    await assertLoggedInAccountState();
  });

  it('returns to guest sign-in state after logout', async () => {
    await logoutIfLoggedIn();
    await assertGuestAccountState();
  });
});
