import { launchFresh } from '../helpers/app';
import { BasketPage } from '../pages/BasketPage';

describe('Basket — empty state', () => {
  before(async () => {
    await launchFresh();
    const basket = new BasketPage(browser);
    await basket.clearIfNeeded();
    await basket.open();
  });

  it('shows empty basket message', async () => {
    const basket = new BasketPage(browser);
    await basket.expectEmptyState();
  });
});
