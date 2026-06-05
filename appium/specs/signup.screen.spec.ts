import { byTextContains, dismissSystemDialogs, openSignUpFromLogin, textExists, waitForAppReady } from '../helpers/app';
import { UiText } from '../helpers/selectors';

describe('Account — sign up screen fields', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await openSignUpFromLogin();
  });

  it('shows create account title', async () => {
    expect(await textExists(UiText.signupTitle)).toBe(true);
  });

  it('shows person and company account type chips', async () => {
    expect(await textExists('Person')).toBe(true);
    expect(await textExists('Company')).toBe(true);
  });

  it('shows required signup form fields', async () => {
    await expect(byTextContains(UiText.firstName)).resolves.toBeDefined();
    await expect(byTextContains('Surname')).resolves.toBeDefined();
    await expect(byTextContains('Email')).resolves.toBeDefined();
    await expect(byTextContains('Address')).resolves.toBeDefined();
    await expect(byTextContains('Password')).resolves.toBeDefined();
    await expect(byTextContains(UiText.signUp)).resolves.toBeDefined();
  });
});
