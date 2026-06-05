export function hasTestCredentials(): boolean {
  return Boolean(process.env.TEST_EMAIL?.trim() && process.env.TEST_PASSWORD?.trim());
}

export function hasTestResetToken(): boolean {
  return Boolean(process.env.TEST_RESET_TOKEN?.trim());
}

export function hasTestClaimToken(): boolean {
  return Boolean(process.env.TEST_CLAIM_TOKEN?.trim());
}

export function hasTestPendingOrderId(): boolean {
  return Boolean(process.env.TEST_PENDING_ORDER_ID?.trim());
}

export function hasTestClaimPassword(): boolean {
  return Boolean(process.env.TEST_CLAIM_PASSWORD?.trim());
}

export function hasTestGuestOrderLookup(): boolean {
  return Boolean(process.env.TEST_GUEST_EMAIL?.trim() && process.env.TEST_ORDER_ID?.trim());
}

export function hasTestCredentialsForPush(): boolean {
  return hasTestCredentials();
}
