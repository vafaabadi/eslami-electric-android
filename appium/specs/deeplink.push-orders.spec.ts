import { launchFresh, openPushDeepLink } from '../helpers/app';
import { MyOrdersPage } from '../pages/MyOrdersPage';

describe('Deep link — push orders route', () => {
  before(async () => {
    await launchFresh();
  });

  it('opens My orders via eslamielectric://push/orders adb deep link', async () => {
    await openPushDeepLink('orders');
    const orders = new MyOrdersPage(browser);
    await orders.expectScreenVisible();
  });
});
