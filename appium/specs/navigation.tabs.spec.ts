import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';
import { BasketPage } from '../pages/BasketPage';
import { HomePage } from '../pages/HomePage';
import { ProductsPage } from '../pages/ProductsPage';

describe('Navigation — bottom tabs', () => {
  before(async () => {
    await launchFresh();
  });

  it('cycles through all four bottom tabs', async () => {
    const home = new HomePage(browser);
    await home.open();
    await home.expectFeaturedSectionVisible();

    const products = new ProductsPage(browser);
    await products.open();
    await products.expectCatalogControlsVisible();

    const basket = new BasketPage(browser);
    await basket.open();

    const account = new AccountPage(browser);
    await account.open();
    await account.expectGuestState();
  });
});
