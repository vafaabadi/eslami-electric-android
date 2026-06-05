import { $, $$, driver } from '@wdio/globals';
import { resourceId, Selectors, UiText } from './selectors';

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

export async function byTextContains(text: string, timeoutMs = DEFAULT_TIMEOUT_MS) {
  const el = await $(`android=new UiSelector().textContains("${text}")`);
  await el.waitForExist({ timeout: timeoutMs });
  return el;
}

export async function textExists(text: string): Promise<boolean> {
  const el = await $(`android=new UiSelector().textContains("${text}")`);
  return el.isExisting();
}

export async function addFirstProductToBasket() {
  await tapNav('navProducts');
  await byTestTag(Selectors.addToBasket, 30_000);
  await firstAddToBasket();
}

export async function ensureBasketHasItem() {
  await tapNav('navBasket');
  const hasLine = await byTestTag(Selectors.basketLineItem, 3_000).then(() => true).catch(() => false);
  if (!hasLine) {
    await addFirstProductToBasket();
    await tapNav('navBasket');
    await byTestTag(Selectors.basketLineItem, 15_000);
  }
}

export async function clearBasketIfNeeded() {
  await tapNav('navBasket');
  for (let i = 0; i < 20; i++) {
    const empty = await byTestTag(Selectors.basketEmpty, 2_000).then(() => true).catch(() => false);
    if (empty) return;
    const remove = await $(`android=new UiSelector().text("${UiText.basketRemove}")`);
    if (!(await remove.isExisting())) return;
    await remove.click();
    await driver.pause(500);
  }
}

export async function tapProceedToCheckout() {
  const btn = await byTextContains(UiText.checkoutProceed);
  await btn.click();
  await driver.pause(800);
}

export async function openCheckoutAsGuest() {
  await ensureBasketHasItem();
  await tapProceedToCheckout();
  await byTextContains(UiText.checkoutTitle, 15_000);
}

export async function openLoginScreen() {
  await tapNav('navAccount');
  await (await byTestTag(Selectors.accountLogin)).click();
  await byTestTag(Selectors.loginEmail, 10_000);
}

export async function openSignUpFromLogin() {
  await openLoginScreen();
  await (await byTextContains(UiText.signUp)).click();
  await byTextContains(UiText.signupTitle, 10_000);
}

export async function openForgotPasswordFromLogin() {
  await openLoginScreen();
  await (await byTextContains(UiText.forgotPasswordLink)).click();
  await byTextContains(UiText.forgotPasswordTitle, 10_000);
}

export async function openMyOrders() {
  await tapNav('navAccount');
  await (await byTextContains(UiText.myOrders)).click();
  await driver.pause(1500);
}

export async function countAddToBasketButtons(): Promise<number> {
  const buttons = await $$(`android=new UiSelector().resourceId("${resourceId(Selectors.addToBasket)}")`);
  return buttons.length;
}
