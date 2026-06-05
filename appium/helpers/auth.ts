import { $, driver } from '@wdio/globals';
import { byTestTag, hideKeyboardIfOpen, waitForAppReady } from './app';
import { Selectors } from './selectors';

export function hasTestCredentials(): boolean {
  return Boolean(process.env.TEST_EMAIL?.trim() && process.env.TEST_PASSWORD?.trim());
}

export async function loginWithEnvCredentials() {
  if (!hasTestCredentials()) {
    throw new Error('TEST_EMAIL and TEST_PASSWORD must be set in appium/.env');
  }

  await waitForAppReady();
  await byTestTag(Selectors.navAccount).then((el) => el.click());
  await byTestTag(Selectors.accountLogin).then((el) => el.click());

  const email = await byTestTag(Selectors.loginEmail);
  await email.setValue(process.env.TEST_EMAIL!);
  const password = await byTestTag(Selectors.loginPassword);
  await password.setValue(process.env.TEST_PASSWORD!);
  await hideKeyboardIfOpen();
  await byTestTag(Selectors.loginSubmit).then((el) => el.click());

  await driver.waitUntil(
    async () => {
      try {
        const submit = await byTestTag(Selectors.loginSubmit, 2_000);
        return !(await submit.isDisplayed());
      } catch {
        return true;
      }
    },
    { timeout: 30_000, timeoutMsg: 'Login did not complete' }
  );
}

export async function logoutIfLoggedIn() {
  await byTestTag(Selectors.navAccount).then((el) => el.click());
  const logoutText = await $('android=new UiSelector().textContains("Log out")');
  if (await logoutText.isExisting()) {
    await logoutText.click();
    await driver.pause(800);
  }
}

export async function assertGuestAccountState() {
  await byTestTag(Selectors.navAccount).then((el) => el.click());
  await byTestTag(Selectors.accountLogin, 10_000);
}

export async function assertLoggedInAccountState() {
  await byTestTag(Selectors.navAccount).then((el) => el.click());
  const myOrders = await $('android=new UiSelector().textContains("My orders")');
  await myOrders.waitForExist({ timeout: 15_000 });
}

export async function openProfileFromAccount() {
  await byTestTag(Selectors.navAccount).then((el) => el.click());
  const profileBtn = await $('android=new UiSelector().textContains("Profile")');
  await profileBtn.waitForExist({ timeout: 15_000 });
  await profileBtn.click();
  await driver.pause(800);
}
