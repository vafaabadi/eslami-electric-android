import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Account — login screen navigation', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navAccount');
  });

  it('opens login screen from Account tab', async () => {
    await (await byTestTag(Selectors.accountLogin)).click();
    await expect(byTestTag(Selectors.loginEmail)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.loginPassword)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.loginSubmit)).resolves.toBeDefined();
  });
});
