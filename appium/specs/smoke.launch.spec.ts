import { byTestTag, dismissSystemDialogs, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Smoke — app launch', () => {
  it('launches without crash and shows bottom navigation', async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await expect(byTestTag(Selectors.navProducts)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.navBasket)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.navAccount)).resolves.toBeDefined();
  });
});
