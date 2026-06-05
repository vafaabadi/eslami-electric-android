import { driver } from '@wdio/globals';
import {
  byTestTag,
  countAddToBasketButtons,
  dismissSystemDialogs,
  tapNav,
  waitForAppReady,
} from '../helpers/app';
import { Selectors, UiText } from '../helpers/selectors';

describe('Products — search updates grid', () => {
  let baselineCount = 0;

  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navProducts');
    await byTestTag(Selectors.addToBasket, 30_000);
    baselineCount = await countAddToBasketButtons();
  });

  it('narrows results when searching a non-matching term', async () => {
    const search = await byTestTag(Selectors.searchProducts);
    await search.setValue('zzzz-no-match-zzzz');
    await driver.pause(800);
    expect(await countAddToBasketButtons()).toBe(0);
  });

  it('restores product grid after clearing search', async () => {
    const search = await byTestTag(Selectors.searchProducts);
    await search.clearValue();
    await driver.pause(800);
    const restored = await countAddToBasketButtons();
    expect(restored).toBeGreaterThan(0);
    expect(restored).toBe(baselineCount);
  });

  it('filters to matching products for a known partial term', async () => {
    const term = process.env.SEARCH_TERM?.trim() || UiText.searchDefaultTerm;
    const search = await byTestTag(Selectors.searchProducts);
    await search.setValue(term);
    await driver.pause(800);
    const filtered = await countAddToBasketButtons();
    expect(filtered).toBeGreaterThan(0);
    expect(filtered).toBeLessThanOrEqual(baselineCount);
    await search.clearValue();
  });
});
