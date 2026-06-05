import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Home — view all navigates to products', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navHome');
  });

  it('lands on products tab after tapping View all', async () => {
    await (await byTestTag(Selectors.viewAllProducts)).click();
    const search = await byTestTag(Selectors.searchProducts, 15_000);
    await expect(search).toBeDisplayed();
    await expect(byTestTag(Selectors.addToBasket, 15_000)).resolves.toBeDefined();
  });
});
