import { byTestTag, clearBasketIfNeeded, dismissSystemDialogs, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Basket — empty state', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await clearBasketIfNeeded();
    await tapNav('navBasket');
  });

  it('shows empty basket message', async () => {
    const empty = await byTestTag(Selectors.basketEmpty, 10_000);
    await expect(empty).toBeDisplayed();
    const text = await empty.getText();
    expect(text.toLowerCase()).toContain('empty');
  });
});
