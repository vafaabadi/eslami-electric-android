import { launchFresh } from '../helpers/app';
import { hasTestClaimPassword, hasTestClaimToken } from '../helpers/env';
import { AccountPage } from '../pages/AccountPage';

describe('Journey — claim guest account', () => {
  before(async () => {
    await launchFresh();
  });

  it('Account claim entry shows token field and validate CTA', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const claim = await account.openClaimAccount();
    await claim.expectTokenFieldAndValidate();
  });

  it('validates token field interaction', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const claim = await account.openClaimAccount();
    await claim.fillToken('invalid-e2e-token');
    await claim.tapValidate();
    await claim.expectValidationErrorOrReady();
  });

  it('optional full claim with TEST_CLAIM_TOKEN (skip if unset)', async function () {
    if (!hasTestClaimToken()) {
      this.skip();
      return;
    }
    const account = new AccountPage(browser);
    await account.open();
    const claim = await account.openClaimAccount();
    await claim.fillToken(process.env.TEST_CLAIM_TOKEN!);
    await claim.tapValidate();
    if (!hasTestClaimPassword()) {
      await claim.expectValidationErrorOrReady();
      return;
    }
    await claim.fillPasswords(process.env.TEST_CLAIM_PASSWORD!);
    await claim.submitClaim();
  });
});
