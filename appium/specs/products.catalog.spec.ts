import { $$, driver } from '@wdio/globals';
import { byTestTag, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { resourceId, Selectors } from '../helpers/selectors';

describe('Products — catalog search, filters, sort', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navProducts');
  });

  it('shows search field and product grid', async () => {
    const search = await byTestTag(Selectors.searchProducts, 30_000);
    await expect(search).toBeDisplayed();
    await expect(byTestTag(Selectors.addToBasket, 30_000)).resolves.toBeDefined();
  });

  it('filters products via search input', async () => {
    const search = await byTestTag(Selectors.searchProducts);
    await search.setValue('zzzz-no-match-zzzz');
    await driver.pause(800);
    const addButtons = await $$(`android=new UiSelector().resourceId("${resourceId(Selectors.addToBasket)}")`);
    expect(addButtons.length).toBe(0);
    await search.clearValue();
    await driver.pause(800);
  });

  it('selects category and sort chips', async () => {
    const allCategories = await byTestTag(Selectors.chipCategoryAll, 15_000).catch(() => null);
    if (allCategories) {
      await allCategories.click();
    }
    const sortDefault = await byTestTag(Selectors.chipSortDefault);
    await sortDefault.click();
    await expect(sortDefault).toBeDisplayed();
  });
});
