import { launchFresh } from '../helpers/app';
import { HomePage } from '../pages/HomePage';

describe('Home — view all products', () => {
  before(async () => {
    await launchFresh();
  });

  it('navigates from Home featured to Products catalog', async () => {
    const home = new HomePage(browser);
    await home.open();
    const products = await home.openViewAllProducts();
    await products.expectCatalogControlsVisible();
  });
});
