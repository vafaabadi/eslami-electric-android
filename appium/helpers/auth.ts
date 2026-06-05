import { AccountPage } from '../pages/AccountPage';
import { LoginPage } from '../pages/LoginPage';
import { launchFresh } from './app';

export function hasTestCredentials(): boolean {
  return Boolean(process.env.TEST_EMAIL?.trim() && process.env.TEST_PASSWORD?.trim());
}

export async function loginWithEnvCredentials() {
  if (!hasTestCredentials()) {
    throw new Error('TEST_EMAIL and TEST_PASSWORD must be set in appium/.env');
  }

  await launchFresh();
  const account = new AccountPage(browser);
  await account.open();
  const login = await account.openLogin();
  await login.login(process.env.TEST_EMAIL!, process.env.TEST_PASSWORD!);
}

export async function logoutIfLoggedIn() {
  const account = new AccountPage(browser);
  await account.logoutIfVisible();
}

export async function assertGuestAccountState() {
  const account = new AccountPage(browser);
  await account.expectGuestState();
}

export async function assertLoggedInAccountState() {
  const account = new AccountPage(browser);
  await account.expectLoggedInState();
}

export async function openProfileFromAccount() {
  const account = new AccountPage(browser);
  await account.open();
  return account.openProfile();
}

export async function openLoginPage(): Promise<LoginPage> {
  const account = new AccountPage(browser);
  await account.open();
  return account.openLogin();
}
