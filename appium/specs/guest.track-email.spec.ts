import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Guest — email + order tab validation and ORD auto-switch', () => {
  let guest: import('../pages/GuestTrackOrderPage').GuestTrackOrderPage;

  before(async () => {
    await launchFresh();
    const account = new AccountPage(browser);
    await account.open();
    guest = await account.openGuestTrack();
  });

  it('shows email and order number fields on email tab', async () => {
    await guest.expectEmailTabVisible();
  });

  it('validates empty email lookup submission', async () => {
    await guest.submitEmptyEmailLookup();
    await guest.expectEmailValidationError();
  });

  it('auto-switches from token tab when ORD- number is submitted', async () => {
    await guest.pasteOrderNumberOnTokenTab('ORD-ABC123');
    await guest.expectSwitchedToEmailTabWithOrderRef('ORD-ABC123');
  });
});
