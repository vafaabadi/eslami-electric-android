import { launchFresh } from '../helpers/app';
import { HomePage } from '../pages/HomePage';

describe('Home — featured products', () => {
  let home: HomePage;

  before(async () => {
    await launchFresh();
    home = new HomePage(browser);
    await home.open();
  });

  it('shows featured section on Home tab', async () => {
    await home.expectFeaturedSectionVisible();
  });

  it('lists at least one product with Add to basket', async () => {
    await home.expectAddToBasketVisible();
  });

  it('navigates to Products via View all', async () => {
    const products = await home.openViewAllProducts();
    await products.expectCatalogControlsVisible();
  });
});
