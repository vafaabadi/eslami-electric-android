import { launchFresh } from '../helpers/app';
import { ProductsPage } from '../pages/ProductsPage';

describe('Product detail — navigation from grid', () => {
  before(async () => {
    await launchFresh();
    const products = new ProductsPage(browser);
    await products.open();
  });

  it('opens product detail when tapping a product card', async () => {
    const products = new ProductsPage(browser);
    const detail = await products.openFirstProductDetail();
    await detail.expectVisible();
  });
});
