import type { Browser } from 'webdriverio';
import { UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';

export class OrderDetailPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectOrderDetailVisible(): Promise<void> {
    expect(await this.textExists(UiText.orderDetailTitle)).toBe(true);
    await this.byTextContains(UiText.orderItemsHeading);
  }
}
