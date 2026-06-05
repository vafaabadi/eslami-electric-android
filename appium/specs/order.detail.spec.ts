import { $, driver } from '@wdio/globals';
import { byTextContains, dismissSystemDialogs, openMyOrders, textExists, waitForAppReady } from '../helpers/app';
import { hasTestCredentials, loginWithEnvCredentials } from '../helpers/auth';
import { UiText } from '../helpers/selectors';

describe('Orders — order detail from my orders list', () => {
  before(async function () {
    if (!hasTestCredentials()) {
      this.skip();
      return;
    }
    await dismissSystemDialogs();
    await waitForAppReady();
    await loginWithEnvCredentials();
    await openMyOrders();
  });

  it('opens first order detail when orders exist', async function () {
    const empty = await textExists('You have no orders yet');
    if (empty) {
      this.skip();
      return;
    }

    const orderCard = await $('android=new UiSelector().textMatches("ORD-.*")');
    await orderCard.waitForExist({ timeout: 15_000 });
    await orderCard.click();
    await driver.pause(1200);

    expect(await textExists(UiText.orderDetailTitle)).toBe(true);
    await expect(byTextContains(UiText.orderItemsHeading)).resolves.toBeDefined();
  });
});
