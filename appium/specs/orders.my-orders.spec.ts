import { driver } from '@wdio/globals';
import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { Selectors } from '../helpers/selectors';

describe('Orders — my orders (authenticated)', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await dismissSystemDialogs();
    await waitForAppReady();
    await loginWithEnvCredentials();
  });

  it('opens My orders from Account', async () => {
    await tapNav('navAccount');
    const myOrders = await $('android=new UiSelector().textContains("My orders")');
    await myOrders.waitForExist({ timeout: 15_000 });
    await myOrders.click();
    await driver.pause(1500);
    const listOrEmpty = await $('android=new UiSelector().textContains("order")');
    expect(await listOrEmpty.isExisting()).toBe(true);
  });
});
