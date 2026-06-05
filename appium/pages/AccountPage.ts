import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { GuestTrackOrderPage } from './GuestTrackOrderPage';
import { LoginPage } from './LoginPage';
import { MyOrdersPage } from './MyOrdersPage';
import { NotificationsPage } from './NotificationsPage';
import { ProfilePage } from './ProfilePage';

export class AccountPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async open(): Promise<void> {
    await this.byTestTag(Selectors.navAccount).then((el) => el.click());
    await this.pause(500);
  }

  async openLogin(): Promise<LoginPage> {
    await (await this.byTestTag(Selectors.accountLogin)).click();
    const login = new LoginPage(this.driver);
    await login.expectLoginFormVisible();
    return login;
  }

  async openGuestTrack(): Promise<GuestTrackOrderPage> {
    await (await this.byTestTag(Selectors.guestTrack)).click();
    const guest = new GuestTrackOrderPage(this.driver);
    await guest.expectEmailTabVisible();
    return guest;
  }

  async openMyOrders(): Promise<MyOrdersPage> {
    await (await this.byTextContains(UiText.myOrders)).click();
    await this.pause(1500);
    return new MyOrdersPage(this.driver);
  }

  async openProfile(): Promise<ProfilePage> {
    await (await this.byTextContains(UiText.profileTitle)).click();
    await this.pause(800);
    return new ProfilePage(this.driver);
  }

  async openNotifications(): Promise<NotificationsPage | null> {
    const btn = await this.byTestTagIfExists(Selectors.notificationsBtn, 10_000);
    if (!btn) return null;
    await btn.click();
    const page = new NotificationsPage(this.driver);
    await page.expectScreenVisible();
    return page;
  }

  async expectGuestState(): Promise<void> {
    await this.open();
    await this.byTestTag(Selectors.accountLogin, 10_000);
  }

  async expectLoggedInState(): Promise<void> {
    await this.open();
    await this.byTextContains(UiText.myOrders, 15_000);
  }

  async logoutIfVisible(): Promise<void> {
    await this.open();
    const logout = await this.byTextContains(UiText.logout, 3_000).catch(() => null);
    if (logout) {
      await logout.click();
      await this.pause(800);
    }
  }

  async toggleLocaleEn(): Promise<void> {
    await (await this.byTestTag(Selectors.localeEn)).click();
    await this.pause(600);
  }

  async toggleLocaleFa(): Promise<void> {
    await (await this.byTestTag(Selectors.localeFa)).click();
    await this.pause(600);
  }
}
