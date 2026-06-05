import type { Browser } from 'webdriverio';
import { Selectors, UiText } from '../helpers/selectors';
import { BasePage } from './BasePage';
import { ForgotPasswordPage } from './ForgotPasswordPage';
import { SignUpPage } from './SignUpPage';

export class LoginPage extends BasePage {
  constructor(driver: Browser) {
    super(driver);
  }

  async expectLoginFormVisible(): Promise<void> {
    await this.byTestTag(Selectors.loginEmail, 10_000);
    await this.byTestTag(Selectors.loginPassword);
    await this.byTestTag(Selectors.loginSubmit);
  }

  async fillEmail(email: string): Promise<void> {
    const field = await this.byTestTag(Selectors.loginEmail);
    await field.setValue(email);
  }

  async fillPassword(password: string): Promise<void> {
    const field = await this.byTestTag(Selectors.loginPassword);
    await field.setValue(password);
  }

  async submit(): Promise<void> {
    await this.hideKeyboardIfOpen();
    await (await this.byTestTag(Selectors.loginSubmit)).click();
    await this.driver.waitUntil(
      async () => {
        try {
          const submit = await this.byTestTag(Selectors.loginSubmit, 2_000);
          return !(await submit.isDisplayed());
        } catch {
          return true;
        }
      },
      { timeout: 30_000, timeoutMsg: 'Login did not complete' }
    );
  }

  async login(email: string, password: string): Promise<void> {
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.submit();
  }

  async openSignUp(): Promise<SignUpPage> {
    await (await this.byTextContains(UiText.signUp)).click();
    const signUp = new SignUpPage(this.driver);
    await signUp.expectSignUpFormVisible();
    return signUp;
  }

  async openForgotPassword(): Promise<ForgotPasswordPage> {
    await (await this.byTextContains(UiText.forgotPasswordLink)).click();
    const forgot = new ForgotPasswordPage(this.driver);
    await forgot.expectForgotPasswordVisible();
    return forgot;
  }

  async expectGoogleSignInVisible(): Promise<void> {
    const btn = await this.byTestTagIfExists(Selectors.googleSignIn, 5_000);
    if (btn) {
      await expect(btn).toBeDisplayed();
      return;
    }
    expect(await this.textExists('Google')).toBe(true);
  }
}
