import { launchFresh } from '../helpers/app';
import { hasTestGuestOrderLookup } from '../helpers/env';
import { AccountPage } from '../pages/AccountPage';

describe('Journey — core guest order track lookup', () => {
  before(async function () {
    if (!hasTestGuestOrderLookup()) {
      this.skip();
      return;
    }
    await launchFresh();
  });

  it('looks up guest order by email and order id when env is set', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const guest = await account.openGuestTrack();
    await guest.submitEmailLookup(
      process.env.TEST_GUEST_EMAIL!,
      process.env.TEST_ORDER_ID!
    );
    // Success: order detail or confirmation visible (no hard error toast).
    const error = await $('android=new UiSelector().textContains("not found")');
    const found = await $('android=new UiSelector().textContains("Order")');
    const hasResult = (await found.isExisting()) && !(await error.isExisting());
    expect(hasResult).toBe(true);
  });
});
