import { launchFresh } from '../helpers/app';
import { NavigationBar } from '../pages/NavigationBar';

describe('Smoke — app launch', () => {
  it('launches without crash and shows bottom navigation', async () => {
    await launchFresh();
    const nav = new NavigationBar(browser);
    await nav.expectAllTabsVisible();
  });
});
