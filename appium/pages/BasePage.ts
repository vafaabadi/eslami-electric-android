import { $, $$, driver } from '@wdio/globals';
import type { Browser } from 'webdriverio';
import { resourceId } from '../helpers/selectors';

export const DEFAULT_TIMEOUT_MS = 20_000;

export class BasePage {
  constructor(protected driver: Browser) {}

  protected async byTestTag(tag: string, timeoutMs = DEFAULT_TIMEOUT_MS) {
    const el = await $(`android=new UiSelector().resourceId("${resourceId(tag)}")`);
    await el.waitForExist({ timeout: timeoutMs });
    return el;
  }

  protected async byTestTagIfExists(tag: string, timeoutMs = 3_000) {
    try {
      return await this.byTestTag(tag, timeoutMs);
    } catch {
      return null;
    }
  }

  protected async byTextContains(text: string, timeoutMs = DEFAULT_TIMEOUT_MS) {
    const el = await $(`android=new UiSelector().textContains("${text}")`);
    await el.waitForExist({ timeout: timeoutMs });
    return el;
  }

  protected async byTextExact(text: string, timeoutMs = DEFAULT_TIMEOUT_MS) {
    const el = await $(`android=new UiSelector().text("${text}")`);
    await el.waitForExist({ timeout: timeoutMs });
    return el;
  }

  protected async textExists(text: string): Promise<boolean> {
    const el = await $(`android=new UiSelector().textContains("${text}")`);
    return el.isExisting();
  }

  protected async allByTestTag(tag: string) {
    return $$(`android=new UiSelector().resourceId("${resourceId(tag)}")`);
  }

  protected async pause(ms: number) {
    await driver.pause(ms);
  }

  protected async hideKeyboardIfOpen() {
    try {
      if (await driver.isKeyboardShown()) {
        await driver.hideKeyboard();
      }
    } catch {
      // ignore
    }
  }

  protected async scrollDown() {
    await driver.execute('mobile: scrollGesture', {
      left: 100,
      top: 400,
      width: 200,
      height: 400,
      direction: 'down',
      percent: 0.75,
    });
  }
}
