import { $, driver } from '@wdio/globals';
import { AccountPage } from '../pages/AccountPage';
import { BasketPage } from '../pages/BasketPage';
import { CheckoutResultPage } from '../pages/CheckoutResultPage';
import { HomePage } from '../pages/HomePage';
import { NavigationBar } from '../pages/NavigationBar';
import { APP_ACTIVITY, APP_PACKAGE } from './selectors';

export async function dismissSystemDialogs() {
  const allow = await $('id=com.android.permissioncontroller:id/permission_allow_button');
  if (await allow.isExisting()) {
    await allow.click();
  }
}

export async function waitForAppReady() {
  const nav = new NavigationBar(browser);
  await nav.expectReady();
}

export async function launchFresh() {
  await dismissSystemDialogs();
  await waitForAppReady();
}

export async function openDeepLink(uri: string) {
  await driver.execute('mobile: shell', {
    command: 'am',
    args: [
      'start',
      '-a',
      'android.intent.action.VIEW',
      '-d',
      uri,
      `${APP_PACKAGE}/${APP_ACTIVITY}`,
    ],
  });
  await driver.pause(2000);
}

export async function openCheckoutResultDeepLink(options: {
  success: boolean;
  orderNumber?: string;
  orderId?: string;
  guestToken?: string;
}) {
  const params = new URLSearchParams({
    success: String(options.success),
    orderNumber: options.orderNumber ?? 'ORD-E2E-TEST',
    orderId: options.orderId ?? 'e2e-order-id',
    guestToken: options.guestToken ?? 'e2e-guest-token',
  });
  await openDeepLink(`eslamielectric://checkout-result?${params.toString()}`);
  return new CheckoutResultPage(browser);
}

export async function openPushDeepLink(route: 'orders' | 'basket' | `order/${string}`) {
  await openDeepLink(`eslamielectric://push/${route}`);
}

export async function openCheckoutAsGuest() {
  const basket = new BasketPage(browser);
  await basket.ensureHasItem();
  return basket.proceedToCheckout();
}

export async function openLoginScreen() {
  const account = new AccountPage(browser);
  await account.open();
  return account.openLogin();
}

export async function openSignUpFromLogin() {
  const login = await openLoginScreen();
  return login.openSignUp();
}

export async function openForgotPasswordFromLogin() {
  const login = await openLoginScreen();
  return login.openForgotPassword();
}

export async function openMyOrders() {
  const account = new AccountPage(browser);
  await account.open();
  return account.openMyOrders();
}

export async function openProfileFromAccount() {
  const account = new AccountPage(browser);
  await account.open();
  return account.openProfile();
}

export async function clearBasketIfNeeded() {
  const basket = new BasketPage(browser);
  await basket.clearIfNeeded();
}

export async function ensureBasketHasItem() {
  const basket = new BasketPage(browser);
  await basket.ensureHasItem();
}

export async function tapNav(tab: 'navHome' | 'navProducts' | 'navBasket' | 'navAccount') {
  const nav = new NavigationBar(browser);
  await nav.tap(tab);
}

export function pages() {
  return {
    nav: new NavigationBar(browser),
    home: new HomePage(browser),
    basket: new BasketPage(browser),
    account: new AccountPage(browser),
  };
}
