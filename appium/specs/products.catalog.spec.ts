import { launchFresh } from '../helpers/app';
import { ProductsPage } from '../pages/ProductsPage';

describe('Products — catalog browse', () => {
  before(async () => {
    await launchFresh();
  });

  it('shows search field and filter chips', async () => {
    const products = new ProductsPage(browser);
    await products.open();
    await products.expectCatalogControlsVisible();
  });
});
