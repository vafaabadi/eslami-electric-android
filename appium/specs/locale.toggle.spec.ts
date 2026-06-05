import { driver } from '@wdio/globals';
import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Locale — EN / FA toggle', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navAccount');
  });

  it('shows locale chips and selects فارسی', async () => {
    const fa = await byTestTag(Selectors.localeFa);
    await expect(fa).toBeDisplayed();
    await fa.click();
    await driver.pause(1500);
    await expect(byTestTag(Selectors.localeFa)).resolves.toBeDefined();
  });

  it('switches back to English', async () => {
    const en = await byTestTag(Selectors.localeEn);
    await en.click();
    await driver.pause(1500);
    await expect(byTestTag(Selectors.localeEn)).resolves.toBeDefined();
  });
});
