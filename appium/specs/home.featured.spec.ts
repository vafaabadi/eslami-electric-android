import { byTestTag, dismissSystemDialogs, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Home — featured products', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
  });

  it('shows featured section on Home tab', async () => {
    await (await byTestTag(Selectors.navHome)).click();
    const featured = await byTestTag(Selectors.homeFeatured, 30_000);
    await expect(featured).toBeDisplayed();
  });

  it('lists at least one product with Add to basket', async () => {
    const addBtn = await byTestTag(Selectors.addToBasket, 30_000);
    await expect(addBtn).toBeDisplayed();
  });

  it('navigates to Products via View all', async () => {
    await (await byTestTag(Selectors.viewAllProducts)).click();
    await expect(byTestTag(Selectors.searchProducts)).resolves.toBeDefined();
  });
});
