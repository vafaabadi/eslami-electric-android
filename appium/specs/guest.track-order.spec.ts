import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Guest — track order screen', () => {
  let guest: import('../pages/GuestTrackOrderPage').GuestTrackOrderPage;

  before(async () => {
    await launchFresh();
    const account = new AccountPage(browser);
    await account.open();
    guest = await account.openGuestTrack();
  });

  it('shows email + order number mode by default', async () => {
    await guest.expectEmailTabVisible();
  });

  it('switches to tracking token tab', async () => {
    await guest.switchToTokenTab();
    await guest.switchToEmailTab();
  });

  it('shows validation when submitting empty email lookup', async () => {
    await guest.submitEmptyEmailLookup();
    await guest.expectEmailValidationError();
  });
});
