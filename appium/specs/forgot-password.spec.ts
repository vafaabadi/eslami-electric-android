import { byTextContains, dismissSystemDialogs, openForgotPasswordFromLogin, textExists, waitForAppReady } from '../helpers/app';
import { UiText } from '../helpers/selectors';

describe('Account — forgot password screen', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await openForgotPasswordFromLogin();
  });

  it('shows forgot password title and hint', async () => {
    expect(await textExists(UiText.forgotPasswordTitle)).toBe(true);
    expect(await textExists('reset link')).toBe(true);
  });

  it('shows email field and send reset link button', async () => {
    await expect(byTextContains('Email')).resolves.toBeDefined();
    await expect(byTextContains(UiText.sendResetLink)).resolves.toBeDefined();
  });
});
