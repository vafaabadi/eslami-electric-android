import { launchFresh } from '../helpers/app';
import { UiText } from '../helpers/selectors';
import { ProductsPage } from '../pages/ProductsPage';

describe('Products — search narrows grid', () => {
  let products: ProductsPage;

  before(async () => {
    await launchFresh();
    products = new ProductsPage(browser);
    await products.open();
  });

  it('narrows product grid when searching', async () => {
    await products.searchAndExpectNarrowed(process.env.SEARCH_TERM || UiText.searchDefaultTerm);
  });

  it('restores grid when search is cleared', async () => {
    await products.clearSearchAndExpectRestored();
  });
});
