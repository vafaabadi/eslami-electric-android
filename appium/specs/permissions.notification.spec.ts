import { dismissSystemDialogs, waitForAppReady } from '../helpers/app';

/**
 * Notification permission dialogs are OS-controlled and flaky in CI.
 * This spec documents the flow and skips unless RUN_PERMISSION_SPEC=true.
 */
describe('Permissions — notification prompt (optional)', () => {
  before(function () {
    if (process.env.RUN_PERMISSION_SPEC !== 'true') {
      this.skip();
    }
  });

  it('handles post-notifications allow dialog if shown', async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    const allow = await $('id=com.android.permissioncontroller:id/permission_allow_button');
    if (await allow.isExisting()) {
      await allow.click();
    }
  });
});
