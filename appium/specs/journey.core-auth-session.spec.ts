import { launchFresh, terminateAndRelaunch } from '../helpers/app';
import {
  assertLoggedInAccountState,
  hasTestCredentials,
  loginWithEnvCredentials,
} from '../helpers/auth';

describe('Journey — core auth session persistence', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await launchFresh();
    await loginWithEnvCredentials();
  });

  it('keeps session after terminate and relaunch', async () => {
    await assertLoggedInAccountState();
    await terminateAndRelaunch();
    await assertLoggedInAccountState();
  });
});
