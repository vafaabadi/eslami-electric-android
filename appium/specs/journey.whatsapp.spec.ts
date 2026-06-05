import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';
import { WhatsAppSupportPage } from '../pages/WhatsAppSupportPage';

describe('Journey — WhatsApp FAB and Account contact', () => {
  before(async () => {
    await launchFresh();
  });

  it('shows WhatsApp FAB on home and tap does not crash app', async () => {
    const whatsapp = new WhatsAppSupportPage(browser);
    await whatsapp.expectFabVisibleOnHome();
    await whatsapp.tapFabWithoutCrash();
  });

  it('shows Account WhatsApp button and tap does not crash app', async () => {
    const account = new AccountPage(browser);
    await account.open();
    await account.expectWhatsAppButtonVisible();

    const whatsapp = new WhatsAppSupportPage(browser);
    await whatsapp.tapAccountWhatsAppWithoutCrash();
  });
});
