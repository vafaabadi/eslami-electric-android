import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Guest — token tab paste validation', () => {
  let guest: import('../pages/GuestTrackOrderPage').GuestTrackOrderPage;

  before(async () => {
    await launchFresh();
    const account = new AccountPage(browser);
    await account.open();
    guest = await account.openGuestTrack();
  });

  it('shows error when submitting invalid tracking token', async () => {
    await guest.submitInvalidToken();
    await guest.expectTokenLookupError();
  });
});
