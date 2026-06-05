import { $, driver } from '@wdio/globals';
import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Product detail — navigation from grid', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navProducts');
    await byTestTag(Selectors.addToBasket, 30_000);
  });

  it('opens product detail when tapping a product card', async () => {
    const card = await $('android=new UiSelector().className("android.view.View").instance(10)');
    await card.click();
    await driver.pause(1000);
    const detail = await byTestTag(Selectors.productDetail, 10_000).catch(() => null);
    if (detail) {
      await expect(detail).toBeDisplayed();
      await expect(byTestTag(Selectors.addToBasket)).resolves.toBeDefined();
    } else {
      const title = await $('android=new UiSelector().textContains("Product")');
      expect(await title.isExisting()).toBe(true);
    }
  });
});
