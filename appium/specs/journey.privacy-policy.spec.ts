import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';
import { PrivacyPolicyPage } from '../pages/PrivacyPolicyPage';

describe('Journey — privacy policy Custom Tab', () => {
  before(async () => {
    await launchFresh();
  });

  it('Account → Privacy policy → back returns to app', async () => {
    const account = new AccountPage(browser);
    await account.open();
    await account.expectPrivacyPolicyButtonVisible();

    const privacy = new PrivacyPolicyPage(browser);
    await privacy.tapPrivacyPolicyFromAccount();
    await privacy.expectExternalViewOpened();
    await privacy.returnToApp();
    await privacy.expectAccountScreenAfterBack();
  });
});
