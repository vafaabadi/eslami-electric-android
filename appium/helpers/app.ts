import { $, $$, driver } from '@wdio/globals';
import { resourceId, Selectors } from './selectors';

const DEFAULT_TIMEOUT_MS = 20_000;

export async function byTestTag(tag: string, timeoutMs = DEFAULT_TIMEOUT_MS) {
  const el = await $(`android=new UiSelector().resourceId("${resourceId(tag)}")`);
  await el.waitForExist({ timeout: timeoutMs });
  return el;
}

export async function tapNav(tab: 'navHome' | 'navProducts' | 'navBasket' | 'navAccount') {
  const el = await byTestTag(Selectors[tab]);
  await el.click();
  await driver.pause(500);
}

export async function waitForAppReady() {
  await byTestTag(Selectors.navHome, 30_000);
}

export async function dismissSystemDialogs() {
  const allow = await $('id=com.android.permissioncontroller:id/permission_allow_button');
  if (await allow.isExisting()) {
    await allow.click();
  }
}

export async function hideKeyboardIfOpen() {
  try {
    if (await driver.isKeyboardShown()) {
      await driver.hideKeyboard();
    }
  } catch {
    // ignore
  }
}

export async function firstAddToBasket(timeoutMs = DEFAULT_TIMEOUT_MS) {
  const buttons = await $$(`android=new UiSelector().resourceId("${resourceId(Selectors.addToBasket)}")`);
  await buttons[0].waitForExist({ timeout: timeoutMs });
  await buttons[0].click();
}

export async function getTextByTag(tag: string): Promise<string> {
  const el = await byTestTag(tag);
  return el.getText();
}
