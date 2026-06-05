import { byTextContains, dismissSystemDialogs, textExists, waitForAppReady } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials, openProfileFromAccount } from '../helpers/auth';
import { UiText } from '../helpers/selectors';

describe('Account — profile view and edit fields', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await dismissSystemDialogs();
    await waitForAppReady();
    await loginWithEnvCredentials();
    await openProfileFromAccount();
  });

  it('shows profile screen title', async () => {
    expect(await textExists(UiText.profileTitle)).toBe(true);
  });

  it('shows editable profile fields', async () => {
    await expect(byTextContains(UiText.firstName)).resolves.toBeDefined();
    await expect(byTextContains('Surname')).resolves.toBeDefined();
    await expect(byTextContains('Mobile')).resolves.toBeDefined();
    await expect(byTextContains('Address')).resolves.toBeDefined();
    await expect(byTextContains(UiText.saveProfile)).resolves.toBeDefined();
  });
});
