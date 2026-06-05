import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Navigation — bottom tab cycle', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
  });

  it('loads Home tab', async () => {
    await tapNav('navHome');
    await expect(byTestTag(Selectors.homeFeatured, 30_000)).resolves.toBeDefined();
  });

  it('loads Products tab', async () => {
    await tapNav('navProducts');
    await expect(byTestTag(Selectors.searchProducts, 15_000)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.addToBasket, 30_000)).resolves.toBeDefined();
  });

  it('loads Basket tab', async () => {
    await tapNav('navBasket');
    const emptyOrLine =
      (await byTestTag(Selectors.basketEmpty, 5_000).then(() => true).catch(() => false)) ||
      (await byTestTag(Selectors.basketLineItem, 5_000).then(() => true).catch(() => false));
    expect(emptyOrLine).toBe(true);
  });

  it('loads Account tab', async () => {
    await tapNav('navAccount');
    await expect(byTestTag(Selectors.accountLogin)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.guestTrack)).resolves.toBeDefined();
  });
});
