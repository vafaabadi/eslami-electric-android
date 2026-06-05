import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

const runPermissionSpec = process.env.RUN_PERMISSION_SPEC === 'true';

(runPermissionSpec ? describe : describe.skip)('Permissions — OS notification dialog', () => {
  before(async () => {
    await launchFresh();
  });

  it('handles notification permission prompt when present', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const notifications = await account.openNotifications();
    if (!notifications) return;
    await notifications.expectScreenVisible();
  });
});
