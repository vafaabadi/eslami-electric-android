import { $, driver } from '@wdio/globals';
import { byTestTag, dismissSystemDialogs, hideKeyboardIfOpen, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Guest — email + order tab validation and ORD auto-switch', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navAccount');
    await (await byTestTag(Selectors.guestTrack)).click();
  });

  it('shows email and order number fields on email tab', async () => {
    await expect(byTestTag(Selectors.guestEmail)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.guestOrderRef)).resolves.toBeDefined();
  });

  it('validates empty email lookup submission', async () => {
    await (await byTestTag(Selectors.guestTrackSubmit)).click();
    await hideKeyboardIfOpen();
    const error = await $('android=new UiSelector().textContains("email")');
    expect(await error.isExisting()).toBe(true);
  });

  it('auto-switches from token tab when ORD- number is submitted', async () => {
    await (await byTestTag(Selectors.guestTrackModeToken)).click();
    const tokenField = await byTestTag(Selectors.guestToken);
    await tokenField.setValue('ORD-ABC123');
    await (await byTestTag(Selectors.guestTrackSubmit)).click();
    await driver.pause(600);

    await expect(byTestTag(Selectors.guestOrderRef)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.guestEmail)).resolves.toBeDefined();
    const orderRefField = await byTestTag(Selectors.guestOrderRef);
    const value = await orderRefField.getText();
    expect(value.toUpperCase()).toContain('ORD-ABC123');
  });
});
