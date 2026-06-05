import { launchFresh, openForgotPasswordFromLogin, openResetPasswordDeepLink } from '../helpers/app';
import { hasTestResetToken } from '../helpers/env';

describe('Journey — forgot and reset password', () => {
  describe('forgot password form', () => {
    let forgot: import('../pages/ForgotPasswordPage').ForgotPasswordPage;

    before(async () => {
      await launchFresh();
      forgot = await openForgotPasswordFromLogin();
    });

    it('shows forgot password screen with email field and send link CTA', async () => {
      await forgot.expectHintAndFields();
    });

    it('validates empty and invalid email without leaving screen', async () => {
      await forgot.submitEmptyAndExpectValidation();
      await forgot.submitInvalidEmailAndExpectError();
    });
  });

  describe('reset password via deep link', () => {
    before(async function () {
      if (!hasTestResetToken()) {
        this.skip();
        return;
      }
      await launchFresh();
    });

    it('opens reset password screen from adb deep link with token', async function () {
      const reset = await openResetPasswordDeepLink(process.env.TEST_RESET_TOKEN!);
      await reset.expectResetPasswordVisible();
      await reset.expectFormFields();
    });
  });
});
