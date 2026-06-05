export const APP_PACKAGE = 'com.eslamielectric.android';

/** Compose testTag exposed via semantics { testTagsAsResourceId = true } */
export function resourceId(testTag: string): string {
  return `${APP_PACKAGE}:id/${testTag}`;
}

export const Selectors = {
  navHome: 'nav_tab_home',
  navProducts: 'nav_tab_products',
  navBasket: 'nav_tab_basket',
  navAccount: 'nav_tab_account',
  homeFeatured: 'home_featured_title',
  viewAllProducts: 'btn_view_all_products',
  searchProducts: 'field_search_products',
  chipCategoryAll: 'chip_category_all',
  chipSortDefault: 'chip_sort_default',
  addToBasket: 'btn_add_to_basket',
  stepperDecrease: 'stepper_decrease',
  stepperIncrease: 'stepper_increase',
  stepperQuantity: 'stepper_quantity',
  basketLineItem: 'basket_line_item',
  basketTotal: 'basket_total',
  basketEmpty: 'basket_empty',
  accountLogin: 'btn_account_login',
  guestTrack: 'btn_guest_track',
  loginEmail: 'field_login_email',
  loginPassword: 'field_login_password',
  loginSubmit: 'btn_login_submit',
  guestTrackModeEmail: 'guest_track_mode_email',
  guestTrackModeToken: 'guest_track_mode_token',
  guestEmail: 'field_guest_email',
  guestOrderRef: 'field_guest_order_ref',
  guestToken: 'field_guest_token',
  guestTrackSubmit: 'btn_guest_track_submit',
  localeEn: 'locale_en',
  localeFa: 'locale_fa',
  productDetail: 'screen_product_detail',
  notificationsScreen: 'screen_notifications',
  notificationsMaster: 'toggle_notifications_master',
  notificationsOrders: 'toggle_notifications_orders',
  notificationsBtn: 'btn_notifications',
} as const;

/** Visible UI strings (English default locale) when no testTag exists */
export const UiText = {
  checkoutProceed: 'Proceed to checkout',
  checkoutTitle: 'Checkout',
  checkoutDelivery: 'Delivery',
  checkoutCollection: 'Collection',
  checkoutGuestDetails: 'Guest details',
  checkoutShippingAddress: 'Delivery address',
  checkoutPayStripe: 'Pay with Stripe',
  signupTitle: 'Create account',
  firstName: 'First name',
  signUp: 'Sign up',
  forgotPasswordTitle: 'Forgot password',
  forgotPasswordLink: 'Forgot password?',
  sendResetLink: 'Send reset link',
  profileTitle: 'Profile',
  saveProfile: 'Save profile',
  myOrders: 'My orders',
  orderDetailTitle: 'Order details',
  orderItemsHeading: 'Items',
  basketEmpty: 'Your basket is empty',
  basketRemove: 'Remove',
  logout: 'Log out',
  accountLogin: 'Sign in',
  guestTrackOrderRef: 'Order number',
  searchDefaultTerm: 'a',
} as const;

export type SelectorKey = keyof typeof Selectors;
