import { byTestTag, dismissSystemDialogs, hideKeyboardIfOpen, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Guest — track order screen', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navAccount');
    await (await byTestTag(Selectors.guestTrack)).click();
  });

  it('shows email + order number mode by default', async () => {
    await expect(byTestTag(Selectors.guestEmail)).resolves.toBeDefined();
    await expect(byTestTag(Selectors.guestOrderRef)).resolves.toBeDefined();
  });

  it('switches to tracking token tab', async () => {
    await (await byTestTag(Selectors.guestTrackModeToken)).click();
    await expect(byTestTag(Selectors.guestToken)).resolves.toBeDefined();
    await (await byTestTag(Selectors.guestTrackModeEmail)).click();
  });

  it('shows validation when submitting empty email lookup', async () => {
    await (await byTestTag(Selectors.guestTrackSubmit)).click();
    await hideKeyboardIfOpen();
    const error = await $('android=new UiSelector().textContains("email")');
    const exists = await error.isExisting();
    expect(exists).toBe(true);
  });
});
