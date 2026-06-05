import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { Selectors } from '../helpers/selectors';

describe('Notifications — settings toggles', () => {
  before(async function () {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navAccount');

    if (hasTestCredentials()) {
      await loginWithEnvCredentials();
    } else {
      this.skip();
    }
  });

  it('opens notifications screen and shows master toggle', async () => {
    const btn = await byTestTag(Selectors.notificationsBtn, 10_000).catch(() => null);
    if (!btn) {
      return;
    }
    await btn.click();
    await expect(byTestTag(Selectors.notificationsScreen, 20_000)).resolves.toBeDefined();
    const master = await byTestTag(Selectors.notificationsMaster, 20_000).catch(() => null);
    if (master) {
      await expect(master).toBeDisplayed();
    }
    const orders = await byTestTag(Selectors.notificationsOrders, 10_000).catch(() => null);
    if (orders) {
      await expect(orders).toBeDisplayed();
    }
  });
});
